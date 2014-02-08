/*  Copyright (C) 2014  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    similarImage is free software: you can redistribute it and/or modify
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
package com.github.dozedoff.similarImage.io;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.hash.PhashWorker;

public class ImageProducerTest {
	private Persistence persistence;
	private PhashWorker phw;
	private ThreadPoolExecutor tpe;
	private ImageProducerObserver ipo;

	private ImageProducer imageProducer;

	private static Path testImage = null;
	private static Path[] images;

	private static final int NUM_OF_TEST_IMAGES = 5;
	private static final int OUTPUT_QUEUE_SIZE = 100;

	@BeforeClass
	public static void setUpClass() throws URISyntaxException {
		testImage = Paths.get(Thread.currentThread().getContextClassLoader().getResource("testImage.jpg").toURI());
		assertThat("Could not load test image", Files.exists(testImage), is(true));

		images = new Path[NUM_OF_TEST_IMAGES];

		for (int i = 0; i < NUM_OF_TEST_IMAGES; i++) {
			images[i] = testImage;
		}
	}

	@Before
	public void setUp() throws Exception {
		persistence = mock(Persistence.class);
		phw = mock(PhashWorker.class);
		tpe = mock(ThreadPoolExecutor.class);
		ipo = mock(ImageProducerObserver.class);

		imageProducer = new ImageProducer(OUTPUT_QUEUE_SIZE, persistence, phw, tpe);
	}

	@After
	public void tearDown() throws Exception {
		imageProducer.shutdown();
	}

	@Test
	public void testClearTotal() throws Exception {
		imageProducer.addToLoad(images);
		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));

		imageProducer.clear();
		assertThat(imageProducer.getTotal(), is(0));
	}

	@Test(timeout = 6000)
	public void testBadFilesTotal() throws Exception {
		when(persistence.isBadFile(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);

		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));
	}

	@Test(timeout = 6000)
	public void testRecordedPathsTotal() throws Exception {
		when(persistence.isPathRecorded(any(Path.class))).thenReturn(true, true, false);

		imageProducer.addToLoad(images);

		assertThat(imageProducer.getTotal(), is(NUM_OF_TEST_IMAGES));
	}

	@Test
	public void testAddToLoadList() throws Exception {
		List<Path> list = Arrays.asList(images);

		imageProducer.addToLoad(list);

		verify(tpe).execute(any(Runnable.class));
	}

	@Test
	public void testAddToLoadListLarge() throws Exception {
		List<Path> list = Collections.nCopies(25, testImage);

		imageProducer.addToLoad(list);

		verify(tpe, times(2)).execute(any(Runnable.class));
	}

	@Test
	public void testGetProcessed() throws Exception {
		List<Path> list = Collections.nCopies(3, testImage);

		imageProducer.addToLoad(list);

		assertThat(imageProducer.getProcessed(), is(3));
	}

	@Test
	public void testGetProcessedLargeBatch() throws Exception {
		List<Path> list = Collections.nCopies(30, testImage);

		imageProducer.addToLoad(list);

		assertThat(imageProducer.getProcessed(), is(30));
	}

	@Test
	public void testGetMaxOutputQueueSize() throws Exception {
		assertThat(imageProducer.getMaxOutputQueueSize(), is(100));
	}

	@Test
	public void testAddGuiUpdateListener() throws Exception {
		List<Path> list = Collections.nCopies(3, testImage);
		imageProducer.addGuiUpdateListener(ipo);

		imageProducer.addToLoad(list);

		verify(ipo).totalProgressChanged(3, 3);
	}

	@Test
	public void testAddGuiUpdateListenerTwoEvents() throws Exception {
		List<Path> list = Collections.nCopies(3, testImage);
		List<Path> list2 = Collections.nCopies(5, testImage);
		imageProducer.addGuiUpdateListener(ipo);

		imageProducer.addToLoad(list);
		imageProducer.addToLoad(list2);

		verify(ipo).totalProgressChanged(3, 3);
		verify(ipo).totalProgressChanged(8, 8);
	}

	@Test
	public void testRemoveGuiUpdateListener() throws Exception {
		List<Path> list = Collections.nCopies(3, testImage);
		imageProducer.addGuiUpdateListener(ipo);
		imageProducer.removeGuiUpdateListener(ipo);

		imageProducer.addToLoad(list);

		verify(ipo, never()).totalProgressChanged(anyInt(), anyInt());
	}
}
