/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.common.model.ui.texteditors;

import java.util.*;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jboss.tools.common.text.xml.ui.TextEditorMessages;
import org.jboss.tools.common.text.xml.xpl.GoToMatchingTagAction;

public abstract class AbstractMultiPageContributor extends MultiPageEditorActionBarContributor {
	protected static final String GO_TO_MATCHING_TAG_ID = "org.eclipse.wst.xml.ui.gotoMatchingTag"; //$NON-NLS-1$

	/** The global actions to be connected with editor actions */
	protected final static String[] ACTIONS= {
		ITextEditorActionConstants.UNDO, 
		ITextEditorActionConstants.REDO,
		ITextEditorActionConstants.CUT,
		ITextEditorActionConstants.COPY,
		ITextEditorActionConstants.PASTE,
		ITextEditorActionConstants.DELETE,
		ITextEditorActionConstants.SELECT_ALL,
		ITextEditorActionConstants.FIND,
		IDEActionFactory.BOOKMARK.getId(),
		IDEActionFactory.ADD_TASK.getId(),
		ITextEditorActionConstants.PRINT,
		ITextEditorActionConstants.REVERT,
		ITextEditorActionConstants.SAVE,
	};

	protected final static String[] STATUSFIELDS = {
		ITextEditorActionConstants.STATUS_CATEGORY_ELEMENT_STATE,
		ITextEditorActionConstants.STATUS_CATEGORY_INPUT_MODE,
		ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION
	};
	
	protected IEditorPart mainPart;
	/**
	 * The active editor part.
	 */
	protected IEditorPart fActiveEditorPart;
	/** 
	 * The find next action.
	 * @since 2.0
	 */
	protected RetargetTextEditorAction fFindNext;
	/** 
	 * The find previous action.
	 * @since 2.0
	 */
	protected RetargetTextEditorAction fFindPrevious;	
	/** 
	 * The incremental find action.
	 * @since 2.0
	 */
	protected RetargetTextEditorAction fIncrementalFind;	
	/**
	 * The reverse incremental find action.
	 * @since 2.1
	 */
	protected RetargetTextEditorAction fIncrementalFindReverse;	
	/**
	 * The go to line action.
	 */
	protected RetargetTextEditorAction fGotoLine;
	/** 
	 * The map of status fields.
	 * @since 2.0
	 */
	protected Map<String,StatusLineContributionItem> fStatusFields;

	protected RetargetTextEditorAction fContentAssistProposal;
	protected RetargetTextEditorAction fContentAssistTip;
	
	protected TextEditorAction fToggleOccurencesMarkUp;
	protected GoToMatchingTagAction fGoToMatchingTagAction;

	public AbstractMultiPageContributor() {
		super();
		createAssistObjects();
		createStatusFields();
	}
	
	protected void createAssistObjects() {

		// JBIDE-2274 There is no any code assist in our XML Editors. >>>
		ResourceBundle resourceBundle = XMLUIMessages.getResourceBundle();
		fContentAssistProposal = new RetargetTextEditorAction(resourceBundle, ""); //$NON-NLS-1$
		fContentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//		fContentAssistProposal = new RetargetTextEditorAction(TextEditorMessages.getResourceBundle(), "ContentAssistProposal."); //$NON-NLS-1$
//		fContentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS); 
		// JBIDE-2274 There is no any code assist in our XML Editors. <<<
		
		fContentAssistTip = new RetargetTextEditorAction(TextEditorMessages.getResourceBundle(), "ContentAssistTip."); //$NON-NLS-1$
		fContentAssistTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
	}

	protected void createStatusFields() {
		fStatusFields = new HashMap<String,StatusLineContributionItem>(3);
		for (int i= 0; i < STATUSFIELDS.length; i++)
			fStatusFields.put(STATUSFIELDS[i], new StatusLineContributionItem(STATUSFIELDS[i]));
	}
	
	/*
	 * @see org.eclipse.ui.IEditorActionBarContributor#init(org.eclipse.ui.IActionBars, org.eclipse.ui.IWorkbenchPage)
	 */
	public void init(IActionBars bars, IWorkbenchPage page) {
		super.init(bars, page);

		IToolBarManager toolBarManager= bars.getToolBarManager();
		if (toolBarManager != null && fToggleOccurencesMarkUp != null) {
			toolBarManager.add(new Separator());
			toolBarManager.add(fToggleOccurencesMarkUp);
		}
	}

	protected void initEditMenu(IActionBars bars) {
		IMenuManager menuManager= bars.getMenuManager();
		IMenuManager editMenu= menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(new Separator());
			editMenu.add(fContentAssistProposal);
			editMenu.add(fContentAssistTip);
		}	
	}

	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		mainPart = part;
		
		IActionBars actionBars = getActionBars();
		
		if (actionBars != null) {
			IStatusLineManager slm = actionBars.getStatusLineManager();
			if (slm != null) {
				slm.setErrorMessage(null);
				slm.setMessage(null);
			} 
		}
	}
	
	
	/**
	 * @param editor
	 * @return
	 */
	protected ITextEditor getTextEditor(IEditorPart editor) {
		ITextEditor textEditor = null;
		if (editor instanceof ITextEditor)
			textEditor = (ITextEditor) editor;
		if (textEditor == null && editor != null)
			textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
		return textEditor;
	}

	protected void cleanStatusLine() {
		if (fActiveEditorPart instanceof ITextEditorExtension) {
			ITextEditorExtension extension= (ITextEditorExtension) fActiveEditorPart;
			for (int i= 0; i < STATUSFIELDS.length; i++)
				extension.setStatusField(null, STATUSFIELDS[i]);
		}
	}
	
	protected final void cleanActionBarStatus() {
		IStatusLineManager slm = getActionBars().getStatusLineManager();
		if (slm != null) {
			slm.setErrorMessage(null);
			slm.setMessage(null);
		} 
	}
	
	protected void updateStatus() {
		if (fActiveEditorPart instanceof ITextEditor) {
			ITextEditor textEditor= (ITextEditor) fActiveEditorPart;
			fContentAssistProposal.setAction(getAction(textEditor, "ContentAssistProposal"));	 //$NON-NLS-1$
			fContentAssistTip.setAction(getAction(textEditor, "ContentAssistTip")); //$NON-NLS-1$
		}		
		if (fActiveEditorPart instanceof ITextEditorExtension) {
			ITextEditorExtension extension= (ITextEditorExtension) fActiveEditorPart;
			for (int i= 0; i < STATUSFIELDS.length; i++)
				extension.setStatusField((IStatusField) fStatusFields.get(STATUSFIELDS[i]), STATUSFIELDS[i]);
		}
	}

	public void contributeToStatusLine(IStatusLineManager statusLineManager) {
		super.contributeToStatusLine(statusLineManager);
		for (int i= 0; i < STATUSFIELDS.length; i++)
			statusLineManager.add((IContributionItem) fStatusFields.get(STATUSFIELDS[i]));
	}
	
	protected final IAction getAction(ITextEditor editor, String actionId) {
		return (editor == null ? null : editor.getAction(actionId));
	}
	
	protected static String[] ACTIONS_1 = {
		ITextEditorActionConstants.CUT,
		ITextEditorActionConstants.COPY,
		ITextEditorActionConstants.PASTE,
		ITextEditorActionConstants.DELETE
	};
	protected static String[] ACTIONS_2 = {
		ITextEditorActionConstants.CUT,
		ITextEditorActionConstants.COPY,
		ITextEditorActionConstants.PASTE,
		ITextEditorActionConstants.DELETE,
		ITextEditorActionConstants.UNDO,
		ITextEditorActionConstants.REDO
	};
	
	Map<IAction, ActionHandler> used = new HashMap<IAction, ActionHandler>();
	Map<String, Deactivator> registered = new HashMap<String, Deactivator>();
	
	class Deactivator {
		IHandlerService service;
		IHandlerActivation handler;
		Deactivator(IHandlerService service, IHandlerActivation handler) {
			this.service = service;
			this.handler = handler;
		}
		void deactivate() {
			service.deactivateHandler(handler);
		}
	}

	public void registerKeyBindings(IHandlerService handler, String[] actions, ITextEditor editor) {
		for (int i = 0; i < actions.length; i++) {
			IAction action = editor.getAction(actions[i]);
			registerKeyBinding(handler, actions[i], action);
		}
	}

	protected void registerKeyBinding(IHandlerService handler, String command, IAction action) {
		if(action == null) return;
		if(handler == null) {
			return;
		}
		ActionHandler h = used.get(action);
		if(h == null) {
			h = new ActionHandler(action);
			used.put(action, h);
		}
		String id = action.getId();
		Deactivator c = registered.remove(command);
		if(c != null) c.deactivate();
		IHandlerActivation a = handler.activateHandler(command, h);
		if(a != null) registered.put(command, new Deactivator(handler, a));
	}

	public void dispose() {
		if (fToggleOccurencesMarkUp != null) {
			fToggleOccurencesMarkUp.setEditor(null);
			fToggleOccurencesMarkUp = null;
		}
		super.dispose();
	}
	

	protected class AFakeTextEditor implements ITextEditor, ITextOperationTarget {
		protected HashMap<String,IAction> actions = new HashMap<String,IAction>();

		public AFakeTextEditor() {
			createFakeActions();
		}
		protected void createFakeActions() {
			TextActionHelper.addCutAction(this);
			TextActionHelper.addCopyAction(this);
			TextActionHelper.addPasteAction(this);
			TextActionHelper.addDeleteAction(this);
		}
		public IDocumentProvider getDocumentProvider() {
			return null;
		}
		public void close(boolean save) {}
		public boolean isEditable() {
			return false;
		}
		public void doRevertToSaved() {}
		public void setAction(String actionID, IAction action) {
			actions.put(actionID, action);
		}
		public IAction getAction(String id) {
			return actions.get(id);
		}
		public void setActionActivationCode(String actionId, char activationCharacter, int activationKeyCode, int activationStateMask) {}
		public void removeActionActivationCode(String actionId) {}
		public boolean showsHighlightRangeOnly() {
			return false;
		}
		public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {}
		public void setHighlightRange(int offset, int length, boolean moveCursor) {}
		public IRegion getHighlightRange() {
			return null;
		}
		public void resetHighlightRange() {}
		public ISelectionProvider getSelectionProvider() {
			return null;
		}
		public void selectAndReveal(int offset, int length) {
		}
		public IEditorInput getEditorInput() {
			return null;
		}
		public IEditorSite getEditorSite() {
			return (IEditorSite)(fActiveEditorPart != null ? fActiveEditorPart.getSite() : mainPart.getSite());
		}
		public void gotoMarker(IMarker marker) {
		}
		public void init(IEditorSite site, IEditorInput input) throws PartInitException {}
		public void addPropertyListener(IPropertyListener listener) {}
		public void createPartControl(Composite parent) {}
		public void dispose() {}
		public IWorkbenchPartSite getSite() {
			return fActiveEditorPart != null ? fActiveEditorPart.getSite() : mainPart.getSite();
		}
		public String getTitle() {
			return null;
		}
		public Image getTitleImage() {
			return null;
		}
		public String getTitleToolTip() {
			return null;
		}
		public void removePropertyListener(IPropertyListener listener) {}
		public void setFocus() {}
		public Object getAdapter(Class adapter) {
			if (ITextOperationTarget.class.equals(adapter))	return this;
			return null;
		}
		public void doSave(IProgressMonitor monitor) {}
		public void doSaveAs() {}
		public boolean isDirty() {
			return false;
		}
		public boolean isSaveAsAllowed() {
			return false;
		}
		public boolean isSaveOnCloseNeeded() {
			return false;
		}
		public boolean canDoOperation(int operation) {
			return false;
		}
		public void doOperation(int operation) {}
	}

}
