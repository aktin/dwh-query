package org.aktin.report.archive;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class TestCompletableFutureFailures {

	/**
	 * Make sure that checked exceptions wrapped in a completion exception are unwrapped
	 * and returned by the completion stage.
	 */
	@Test
	public void expectUnwrappedCompletionError(){
		CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
			throw new CompletionException(new FileNotFoundException());
		});
		try{
			f.get();
			Assert.fail();
		}catch( ExecutionException e ){
			Assert.assertEquals(FileNotFoundException.class, e.getCause().getClass());
		} catch (InterruptedException e) {
			Assert.fail();
		}
	}
	
	/**
	 * Make sure that when the second stage throws a {@link CompletionException},
	 * the cause is rewrapped during retrieval in a execution exception. The
	 * third stage whenComplete should not swallow the exception from the second stage.
	 */
	@Test
	public void expectRewrappedInHandler(){
		CompletableFuture<?> f = CompletableFuture.runAsync(() -> {
		}).handle( (v,t) -> {
			throw new CompletionException(new FileNotFoundException());
		}).whenComplete( (v,t) -> {
			Assert.assertNotNull(t);
			Assert.assertEquals(CompletionException.class, t.getClass());
			Assert.assertEquals(FileNotFoundException.class, t.getCause().getClass());
		});
		try{
			f.get();
			Assert.fail();
		}catch( ExecutionException e ){
			Assert.assertEquals(FileNotFoundException.class, e.getCause().getClass());
		}catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	/**
	 * Make sure that unchecked exceptions are wrapped in {@link ExecutionException}
	 * before they are returned by the completion stage.
	 */
	@Test
	public void expectWrappedCompletionError(){
		CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
			throw new AssertionError();
		});
		try{
			f.get();
			Assert.fail();
		}catch( ExecutionException e ){
			Assert.assertEquals(AssertionError.class, e.getCause().getClass());
		} catch (InterruptedException e) {
			Assert.fail();
		}
	}
}
