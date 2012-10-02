package siris.components.editor.filesystem;
/*
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA  02111-1307, USA.
 */


import java.io.File;



/**
 * Interface for listening to disk file changes.
 * @see siris.components.editor.filesystem.FileMonitor
 *
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */
public interface FileListener
{
  /**
   * Called when one of the monitored files are created, deleted
   * or modified.
   *
   * @param file  File which has been changed.
   */
  void fileChanged (File file);
}