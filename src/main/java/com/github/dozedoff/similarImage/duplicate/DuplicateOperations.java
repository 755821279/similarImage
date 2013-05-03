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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;

public class DuplicateOperations {
	private static final Logger logger = LoggerFactory.getLogger(DuplicateOperations.class);
	
	public void moveToDnw(Path path) {
		logger.info("Method not implemented");
		//TODO code me
	}
	
	public void deleteFile(Path path) {
		try {
			Files.delete(path);
			ImageRecord ir = new ImageRecord(path.toString(), 0);
			Persistence.getInstance().deleteRecord(ir);
		} catch (IOException e) {
			logger.warn("Failed to delete {} - {}", path, e.getMessage());
		} catch (SQLException e) {
			logger.warn("Failed to remove {} from database - {}", path, e.getMessage());
		}
	}
}
