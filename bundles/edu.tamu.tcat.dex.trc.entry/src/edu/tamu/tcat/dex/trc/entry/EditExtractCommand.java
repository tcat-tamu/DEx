package edu.tamu.tcat.dex.trc.entry;

import java.util.concurrent.Future;

public interface EditExtractCommand
{

   Future<String> execute() throws DramaticExtractException;
}
