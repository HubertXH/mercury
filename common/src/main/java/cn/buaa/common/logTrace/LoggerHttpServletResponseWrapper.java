package cn.buaa.common.logTrace;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class LoggerHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private ServletOutputStream outputStream = null;
    private PrintWriter printWriter = null;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to be wrapped
     * @throws IllegalArgumentException if the response is null
     */
    public LoggerHttpServletResponseWrapper(HttpServletResponse response)throws IOException {
        super(response);
        outputStream = new LoggerServletOutputStream();
        printWriter = new PrintWriter(new OutputStreamWriter(bytes, this.getCharacterEncoding()));
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }




    @Override
    public void flushBuffer() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    @Override
    public void reset() {
        bytes.reset();
    }


    public byte[] getResponseData() throws IOException {
        flushBuffer();
        return bytes.toByteArray();
    }


    private class LoggerServletOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) throws IOException {
            bytes.write(b);
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }


}
