// Copyright 2014, Steve Tarzia
package com.stevetarzia.PaperSettlers;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

public class Servlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/postscript");
        BoardGenerator boardGenerator = new BoardGenerator(response.getOutputStream());
        boardGenerator.writeRandomPSBoard();
    }
}