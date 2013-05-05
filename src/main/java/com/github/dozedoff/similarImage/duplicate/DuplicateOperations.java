/*  Copyright (C) 2013  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    mmut is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.github.dozedoff.similarImage.duplicate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class DuplicateOperations {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateOperations.class);
	
	public void moveToDnw(Path path) {
		logger.info("Method not implemented");
		//TODO code me
	}
	
	public static void deleteFile(Path path) {
		try {
			logger.info("Deleting file {}", path);
			Files.delete(path);
			ImageRecord ir = new ImageRecord(path.toString(), 0);
			Persistence.getInstance().deleteRecord(ir);
		} catch (IOException e) {
			logger.warn("Failed to delete {} - {}", path, e.getMessage());
		} catch (SQLException e) {
			logger.warn("Failed to remove {} from database - {}", path, e.getMessage());
		}
	}
	
	public static void markAsDnw(Path path) {
		//TODO do this with transaction
		//TODO get "Mark as" strings from options
		final String dnw = "DNW";
		try {
			ImageRecord ir = Persistence.getInstance().getRecord(path);
			if(ir == null) {
				logger.warn("No record found for {}", path);
				return;
			}
			
			long pHash = ir.getpHash();
			logger.info("Adding pHash {} to filter, reason {}", pHash, dnw);
			FilterRecord fr = Persistence.getInstance().getFilter(pHash);
			
			if(fr != null) {
				fr.setReason(dnw);
			} else {
				fr = new FilterRecord(pHash, dnw);
			}
			
			Persistence.getInstance().addFilter(fr);
		} catch (SQLException e) {
			logger.warn("DNW operation failed for {} - {}", path, e.getMessage());
		}
	}
	
	public static void markDirectoryDnw(Path directory) {
		if(! isDirectory(directory)) {
			logger.warn("Directory {} not valid, aborting.", directory);
			return;
		}
		
		File dir = directory.toFile();
		File files[] = dir.listFiles();
		
		for(File f : files){
			markAsDnw(f.toPath());
		}
		
		logger.info("Added {} images from {} to filter list", files.length, directory);
	}
	
	private static boolean isDirectory(Path directory) {
		return directory != null && Files.exists(directory) && Files.isDirectory(directory);
	}
	
	public static void pruneRecords(Path directory) {
		if (! isDirectory(directory)) {
			logger.warn("Directory {} not valid, aborting.", directory);
			return;
		}
		
		try {
			List<ImageRecord> records = Persistence.getInstance().filterByPath(directory);
			LinkedList<Path> toPrune = new LinkedList<Path>();
			
			for(ImageRecord ir : records) {
				Path path = Paths.get(ir.getPath());
				
				if(! Files.exists(path)){
					toPrune.add(path);
				}
			}
			
			logger.info("Found {} non-existant records", toPrune.size());
			
			Object options[] = {"Prune " + toPrune.size() + " records?"};
			JOptionPane pane = new JOptionPane(options, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = pane.createDialog("Prune records");
			dialog.setVisible(true);
			
			if(pane.getValue() != null && (Integer)pane.getValue() == JOptionPane.OK_OPTION) {
				for(Path path : toPrune) {
					ImageRecord ir = new ImageRecord(path.toString(), 0);
					Persistence.getInstance().deleteRecord(ir);
				}
			} else {
				logger.info("User aborted prune operation for {}", directory);
			}
		} catch (SQLException e) {
			logger.warn("Failed to prune records for {} - {}", directory, e.getMessage());
		}
	}
}