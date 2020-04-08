/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.gui.commands;

import java.awt.image.BufferedImage;

import java.io.File;

import javafx.beans.value.ObservableValue;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.tma.TMADataIO;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.plugins.workflow.DefaultScriptableWorkflowStep;
import qupath.lib.plugins.workflow.WorkflowStep;

/**
 * Export TMA summary data.
 * 
 * @author Pete Bankhead
 *
 */
public class TMAExporterCommand implements Runnable {
	
	final private ObservableValue<? extends QuPathViewer> viewerValue;
	private File dirPrevious;
	
	public TMAExporterCommand(final ObservableValue<? extends QuPathViewer> viewerValue) {
		super();
		this.viewerValue = viewerValue;
	}

	@Override
	public void run() {
		QuPathViewer viewer = viewerValue.getValue();
		if (viewer == null)
			return;
		ImageData<BufferedImage> imageData = viewer.getImageData();
		PathObjectHierarchy hierarchy = imageData == null ? null : imageData.getHierarchy();
		if (hierarchy == null || hierarchy.isEmpty() || hierarchy.getTMAGrid() == null || hierarchy.getTMAGrid().nCores() == 0) {
			Dialogs.showErrorMessage("TMA export error", "No TMA data available!");
			return;
		}

		String defaultName = ServerTools.getDisplayableImageName(viewer.getServer());
		File dirBase = dirPrevious;
		if (dirBase == null && imageData.getLastSavedPath() != null)
			dirBase = new File(imageData.getLastSavedPath()).getParentFile();
		if (dirBase != null && !dirBase.isDirectory())
			dirBase = null;
		File file = QuPathGUI.getSharedDialogHelper().promptToSaveFile(null, dirBase, defaultName, "TMA data", ".qptma");
		if (file != null) {
			if (!file.getName().endsWith(".qptma"))
				file = new File(file.getParentFile(), file.getName() + ".qptma");
			double downsample = PathPrefs.getTMAExportDownsample();
			TMADataIO.writeTMAData(file, imageData, viewer.getOverlayOptions(), downsample);
			WorkflowStep step = new DefaultScriptableWorkflowStep("Export TMA data", "exportTMAData(\"" + GeneralTools.escapeFilePath(file.getParentFile().getAbsolutePath()) + "\", " + downsample + ")");
			imageData.getHistoryWorkflow().addStep(step);
//			PathAwtIO.writeTMAData(file, imageData, viewer.getOverlayOptions(), Double.NaN);
			dirPrevious = file.getParentFile();
		}
	}
	
}