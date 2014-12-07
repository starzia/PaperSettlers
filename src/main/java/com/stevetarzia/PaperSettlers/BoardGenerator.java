// Copyright 2004, Steve Tarzia
package com.stevetarzia.PaperSettlers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

class BoardGenerator{
    static final double[] UP = {0,1};
    static final double[] DOWN = {0,-1};
    static final double[] LEFT = {-1,0};
    static final double[] RIGHT = {1,0};
    static final double[] DOWNRIGHT = {Math.sqrt(2)/2,-Math.sqrt(2)/2};
    static final double[] DOWNLEFT = {-0.8,-1};
    static final double SQRT3 = Math.sqrt(3);
    static final double SIN60 = Math.sin(Math.toRadians(60));
    static final double COS60 = Math.cos(Math.toRadians(60));
    //    static final int UNIT = 64;
    //    static final int HEXSIZE = 10 * UNIT;
    static final int DIMENSION = 4; //number of hexes to a side
    static final String SZ = "1.2";
    static int HEXSIZE = (int)((Double.parseDouble(SZ)*400)
            /((2*DIMENSION-1)*1.5+.5));
    static final int[][] ROUNDTILES = 
      { {5, 4},
        {2, 1},
        {6, 5},
        {3, 2},
        {8, 5},
        {10,3},
        {9, 4},
        {12,1},
        {11,2},
        {4, 3},
        {8, 5},
        {10,3},
        {9, 4},
        {4, 3},
        {5, 4},
        {6, 5},
        {3, 2},
        {11,2} };
    //the terrains associated with each index
    static final String[] TERRAIN_NAMES =
    {"3:1","Brick","oRe","Sheep","Lumber","wHeat"};
    static final String[] TERRAIN_LETTERS =
    {"","B","R","S","L","H"};
    //the number of occurences of each terrain type
    static final int[] TERRAIN = {1,3,3,4,4,4};
    //the number of occurences of each port type
    static final int[] PORT = {4,1,1,1,1,1};

    //[TERRAIN type][circleTile number]
    int[][] hexes = new int[19][2];
    //[port type]
    int[][] ocean = new int[18][2];

    OutputStreamWriter fw; // file writer

    /**
     * Object that writes a random board, in PostScript format, to the passed-in OutputStream
     */
    public BoardGenerator(OutputStream fileWriter){
        fw = new OutputStreamWriter(fileWriter);
    }

    public void writeRandomPSBoard() throws IOException{
        assignHexes();

        printHeaders();
        drawHexes();
        drawInstructions();
        closeOutputFile();
    }

    void closeOutputFile() throws IOException{
        fw.write("showpage\n");
        fw.close();
    }

    void assignHexes(){
        int rand=-1;
        for(int i=0; i<hexes.length; i++){
            for(int j=0; j<hexes[i].length; j++)
                hexes[i][j] = -1;
        }
        //assign terrains
        for(int i=0; i<TERRAIN.length; i++){
            for(int j=0; j<TERRAIN[i]; j++){
                //while first time or last random hex was already set
                while( rand == -1 || hexes[rand][0] > -1 )
                    rand = (int)Math.floor( hexes.length * Math.random() );
                //set that hex to TERRAIN i
                hexes[rand][0] = i;
            }
        }
        //assign pointers to circleTiles
        int tmp=0;
        for(int i=0; i<hexes.length; i++){
            //if hex is not desert
            if(hexes[i][0]>0)
                hexes[i][1]=tmp++;
        }

        //assign ports to ocean hexes
        rand=-1;
        for(int i=0; i<ocean.length; i++){
            ocean[i][0] = -2; //terrain type for ocean is -2
            ocean[i][1] = -1;
        }
        int cointoss = (int)Math.floor(2 * Math.random());
        for(int i=0; i<PORT.length; i++){
            for(int j=0; j<PORT[i]; j++){
                //while first time or last random oceanhex was already set
                while( rand == -1 || ocean[rand][1] > -1 )
                    rand = cointoss + 
                        2*(int)Math.floor(0.5*ocean.length*Math.random());
                //set that oceanhex to port i
                ocean[rand][1] = i;
            }
        }

    }


    void drawHexes() throws IOException{
        double[] origin = new double[2];
        double[] pos = new double[2];
        origin[0] = HEXSIZE;
        origin[1] = (((double)DIMENSION)/2.0) * HEXSIZE*SQRT3;
        double[] direction = {Math.cos(-Math.PI/6),Math.sin(-Math.PI/6)}; 
        //unit vector
         pos[0] = origin[0];
        pos[1] = origin[1];

        //hex labeling
        int hexPointer = 0;
        int edgePointer;
        int edgeSize = DIMENSION;
        pos[0]=origin[0]; pos[1]=origin[1];
        
        while(edgeSize > 1){
            edgeSize--;
            
            for(int i=0; i<6; i++){
                edgePointer = 0;
                while(edgePointer < edgeSize){
                    paint(pos,hexPointer);
                    //move along edge
                    displace(pos,direction,SQRT3*HEXSIZE);
                    edgePointer++;
                    hexPointer++;
                }
                //if sixth edge then go back one
                if(i==5)
                    displace(pos,direction,-SQRT3*HEXSIZE);
                //turn
                rotate60(direction);
                //if sixth edge then go forward one
                if(i==5)
                    displace(pos,direction,SQRT3*HEXSIZE);
                
            }
        }
        //now the center one
        paint(pos, hexPointer);
    }

    static double[] centerOfBoard(){
        double[] ret = new double[2];
        ret[0]= HEXSIZE*( 1.5*DIMENSION - 0.5);
        ret[1]= SQRT3*HEXSIZE * (DIMENSION - 0.5);
        return ret;
    }

    static double[][] sixCorners(double[] pos){
        double[][] ret = new double[6][2];
        for (int i=0; i< 6; i++){
            ret[i][0]=pos[0]+HEXSIZE*Math.cos((Math.PI*i)/3.0);
            ret[i][1]=pos[1]+HEXSIZE*Math.sin((Math.PI*i)/3.0);
        }
        return ret;
    }

    static double distance(double[] p1, double[] p2){
        return Math.sqrt(Math.pow(p2[1]-p1[1],2) + Math.pow(p2[0]-p1[0],2));
    }

    static int[] findPorts(double[][] corners){
        double d;
        int[] ret = new int[2];
        double[] best = new double[2];// = {0,distance(corners[0],centerOfBoard())};
        for(int i=0; i<2; i++){
            best[1]=0;
            for(int j=0; j<6; j++){
                d=distance(corners[j],centerOfBoard());
                //System.out.println("i="+i+",j="+j+"d="+d+"b[0]="+best[0]);
                if(best[1]==0 || d<best[1])
                    if(i==0||ret[0]!=j){
                        best[0]=j;
                        best[1]=d;
                    }
            }
            ret[i]=(int)best[0];
        }
        return ret;
    }

    void paint(double[] pos, int hexPointer)
        throws IOException{

        final double[] BLUE = {0.7,0.7,0.9};

        //if an ocean tile
        if(hexPointer<ocean.length){
            drawHex(pos,HEXSIZE,BLUE);

            //if a port tile
            if(dereference(hexPointer)[1]>=0){

                int[] hex = dereference(hexPointer);
                double iconSize = HEXSIZE;

                // case on port type
                switch(hex[1]){
                case 1:
                    drawBrick(pos,iconSize);
                    break;
                case 2:
                    drawOre(pos,iconSize);
                    break;
                case 3:
                    drawSheep(pos,iconSize);
                    break;
                case 4:
                    drawLumber(pos,iconSize);
                    break;
                case 5:
                    drawWheat(pos,iconSize);      
                    break;
                default:
                    /* Port */
                    ///drawCircle(pos,0.7*HEXSIZE);

                    movePen(newD(pos,LEFT,HEXSIZE*.2));
                    label( TERRAIN_NAMES[dereference(hexPointer)[1]] );
                    break;
                }
                
                double[][] corners = sixCorners(pos);
                int[] ports = findPorts( corners );
                drawCircle( corners[ports[0]], HEXSIZE/8);
                drawCircle( corners[ports[1]], HEXSIZE/8);
               
            }
        }
        // else a resource (non-ocean) tile
        else if (hexPointer>=ocean.length){
            //if not desert, ie if land
            if(dereference(hexPointer)[1]>-1){
                //offset
                movePen(newD(pos,LEFT,HEXSIZE*.6));

                //label terrain
                double[] iconPos = newD(pos,RIGHT,HEXSIZE/4);
                int[] hex = dereference(hexPointer);
                double iconSize = Math.sqrt(ROUNDTILES[hex[1]][1])*HEXSIZE/4;
                final double[][] resourceColors = {
                    {1,1,1},
                    {1,0.7,0.7},
                    {0.8,0.8,0.8},
                    {0.7,1,0.7},
                    {0.5,0.9,0.2},
                    {1,1,0.7}
                };
                drawHex(pos,HEXSIZE,resourceColors[hex[0]]);

                // label roundtiles
                movePen(newD(pos,LEFT,HEXSIZE*.6));
                label(ROUNDTILES[dereference(hexPointer)[1]][0]);
                movePen(newD(pos,DOWNLEFT,HEXSIZE*.6));
                for(int i=0; i<ROUNDTILES[hex[1]][1]; i++) label(".");
                movePen(newD(pos,LEFT,HEXSIZE*.6));

                switch(hex[0]){
                case 1:
                    drawBrick(iconPos,iconSize);
                    break;
                case 2:
                    drawOre(iconPos,iconSize);
                    break;
                case 3:
                    drawSheep(iconPos,iconSize);
                    break;
                case 4:
                    drawLumber(iconPos,iconSize);
                    break;
                case 5:
                    drawWheat(iconPos,iconSize);      
                    break;
                default:
                    label(TERRAIN_LETTERS[dereference(hexPointer)[0]]);
                    break;
                }

            }
        }
    }


    void printHeaders() throws IOException{
        //        fw.write("rotate 90\n0 "+(-10*HEXSIZE)+" translate\n");
        //        fw.write("{  0 72 360 arc stroke } def\n" );
        fw.write("%!PS-Adobe-3.0\n");
        fw.write("%%DocumentMedia: default 792 612 0 () ()\n");
        fw.write("%%Orientation: Portrait\n");
        fw.write("%%EndComments\n");

        fw.write("/Times-Roman findfont\n" );
        fw.write("24 scalefont\n" );
        fw.write("setfont\n");        
    }
    void translate(double[] pos) throws IOException{
        fw.write(" "+(double)pos[0]+" "+(double)pos[1]+" translate\n" );
    }
    void movePen(double[] pos) throws IOException{
        fw.write(" "+(double)pos[0]+" "+(double)pos[1]+" moveto\n" );
    }
    void movePenRel(double[] vec) throws IOException{
        fw.write(" "+(double)vec[0]+" "+(double)vec[1]+" rmoveto\n" );
    }
    void penUp() throws IOException{
        fw.write("stroke\n");
    }
    void penDown() throws IOException{
        fw.write("newpath\n");
    }
    void selectPen(int i) throws IOException{
        //fw.write("SP"+i+";");
    }
    void label(String s) throws IOException{
        fw.write(" ("+s+") show\n");
    }
    void label(int i) throws IOException{
        label( String.valueOf(i)  );
    }
    void label(double d) throws IOException{
        label( String.valueOf(d)  );
    }
    void label(double[] pos, String s) throws IOException{
        movePen(pos);
        label(s);
    }

    void drawLine(double[] pos0, double[] pos1)
        throws IOException{
        penDown();
        movePen( pos0 );
        fw.write(" "+(double)pos1[0]+" "+(double)pos1[1]+" lineto\n");
        penUp();
    }
    void drawLine(double[] pos, double r,
                         double x1, double y1, 
                         double x2, double y2) throws IOException{
        drawLine( newD(newD(pos,RIGHT,x1*r),UP,y1*r),
                  newD(newD(pos,RIGHT,x2*r),UP,y2*r) );
    }

    void drawCircle(double[] pos, double r) throws IOException{
        // this should be absolute, not relative movement
        //double neg[] = {-pos[0], -pos[1]};
        //        translate(pos);
        fw.write("newpath "+(double)pos[0]+" "+(double)pos[1]+" "+(double)r+" 0 360 arc closepath stroke\n");
        //        translate(neg);
    }
    //    void drawCircle(double r) throws IOException{
    //        fw.write("doACircle\n");
    //    }
    void drawCircle(double[] pos, double r,
                           double x, double y, double radius) 
        throws IOException{
        drawCircle( newD(newD(pos,RIGHT,x*r),UP,y*r),radius*r); 
    }
    void drawHex(double[] pos, double r, double[] rgb) throws IOException{
        for( int i=0; i<2; i++ ){
            fw.write("newpath\n");
            fw.write(" "+(double)(pos[0]+r)+" "+(double)pos[1]+" moveto\n");
            fw.write(" "+(double)(-r*COS60)+" "+(double)(r*SIN60)+" rlineto\n");        
            fw.write(" "+(double)(-r)+" "+(double)(0)+" rlineto\n");        
            fw.write(" "+(double)(-r*COS60)+" "+(double)(-r*SIN60)+" rlineto\n");        
            fw.write(" "+(double)(r*COS60)+" "+(double)(-r*SIN60)+" rlineto\n");        
            fw.write(" "+(double)(r)+" "+(double)(0)+" rlineto\n");        
            fw.write(" "+(double)(r*COS60)+" "+(double)(r*SIN60)+" rlineto\n");
            fw.write("closepath\n");
            if( i==0 ){
                fw.write(rgb[0]+" "+rgb[1]+" "+rgb[2]+" setrgbcolor fill\n");
                fw.write("0 setgray\n");
            }
            fw.write("stroke\n");
        }
    }
    void carriageReturn(double d) throws IOException{
        //        fw.write("CP0,"+(int)(-.5*d)+";");
    } 
    
    void drawWheat(double[] pos, double r) throws IOException{
        ///penDown();
        ///movePen(pos);
        r*=1.1;
        drawLine(pos,r,0,-.75,0,.66);
        drawLine(pos,r,0,-.8,.4,-.4);
        drawLine(pos,r,.4,-.4,.4,0);
        drawLine(pos,r,.4,0,0,-.4);
        drawLine(pos,r,0,-.4,-.4,0);
        drawLine(pos,r,-.4,0,-.4,.4);
        drawLine(pos,r,-.4,.4,0,0);
        drawLine(pos,r,0,0,.4,.4);
        drawLine(pos,r,.4,.4,.4,.8);
        drawLine(pos,r,.4,.8,0,.4);
        ///penUp();
    }
    void drawLumber(double[] pos, double r) throws IOException{
        ///penDown();
        ///movePen(pos);
        r*=.95;
        drawLine(pos,r,-.6,-.5,0,.8);
        drawLine(pos,r,0,.8,.6,-.5);
        drawLine(pos,r,.6,-.5,-.6,-.5);
        drawLine(pos,r,-.2,-.5,-.2,-.8);
        drawLine(pos,r,-.2,-.8,.2,-.8);
        drawLine(pos,r,.2,-.8,.2,-.5);
        ///penUp();
    }        
    void drawOre(double[] pos, double r) throws IOException{
        ///penDown();
        ///movePen(pos);
        r*=.9;
        drawLine(pos,r,-.4,.6,-.2,.6);
        drawLine(pos,r,-.2,.6,.8,0);
        drawLine(pos,r,.8,0,.7,-.4);
        drawLine(pos,r,.7,-.4,0,-.5);
        drawLine(pos,r,0,-.5,-.8,-.2);
        drawLine(pos,r,-.8,-.2,-.4,.6);
        drawLine(pos,r,-.2,.6,-.3,0);
        drawLine(pos,r,-.3,0,-.8,-.2);
        drawLine(pos,r,-.3,0,.3,-.1);
        drawLine(pos,r,.3,-.1,0,-.5);
        drawLine(pos,r,.3,-.1,.8,0);
        drawLine(pos,r,.3,-.1,.7,-.4);
        ///penUp();
    }
    void drawBrick(double[ ] pos, double r) throws IOException{
        ///penDown();
        ///movePen(pos);
        r*=.75;
        drawLine(pos,r,-.6,.4,1,.4);
        drawLine(pos,r,1,.4,1,0);
        drawLine(pos,r,1,0,-1,0);
        drawLine(pos,r,-.6,0,-.6,.4);
        drawLine(pos,r,.2,.4,.2,0);
        drawLine(pos,r,-1,0,-1,-.4);
        drawLine(pos,r,-1,-.4,.6,-.4);
        drawLine(pos,r,.6,-.4,.6,0);
        drawLine(pos,r,-.2,0,-.2,-.4);
        drawLine(pos,r,-.6,-.4,-.6,-.8);
        drawLine(pos,r,-.6,-.8,.2,-.8);
        drawLine(pos,r,.2,-.8,.2,-.4);
        ///penUp();
    }
    void drawSheep(double[ ] pos, double r) throws IOException{
        r*=1.1;
        ///penDown();
        ///movePen(pos);
        drawCircle(pos,r,-.2,.15,.2);
        drawCircle(pos,r,.1,.1,.15);
        drawCircle(pos,r,0,-.2,.2);
        drawCircle(pos,r,0.05,.4,.15);
        drawCircle(pos,r,0.4,.3,.22);
        drawCircle(pos,r,0.3,-.2,.2);
        drawCircle(pos,r,.5,0,.1);
        drawCircle(pos,r,-.3,-.2,.15);
        drawLine(pos,r,-.3,-.4,-.3,-.6);
        drawLine(pos,r,-.2,-.6,-.2,-.4);
        drawLine(pos,r,.2,-.4,.2,-.6);
        drawLine(pos,r,.3,-.6,.3,-.4);
        ///penUp();
    }

    void drawInstructions() throws IOException{
        double[] pos =newD(newD(centerOfBoard(),centerOfBoard(),1.0),DOWNRIGHT,1*HEXSIZE);
        label(pos,"Costs:");
        displace(pos,DOWN,1.5*HEXSIZE);

        label(pos,"Road");
        displace(pos,RIGHT,2*HEXSIZE);
        drawBrick(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawLumber(pos,HEXSIZE/2);
        displace(pos,LEFT,3*HEXSIZE);
        displace(pos,DOWN,1.5*HEXSIZE);

        label(pos,"Settlement");
        displace(pos,DOWN,.75*HEXSIZE);
        displace(pos,RIGHT,2*HEXSIZE);        drawBrick(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawLumber(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawWheat(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawSheep(pos,HEXSIZE/2);
        displace(pos,LEFT,5*HEXSIZE);
        displace(pos,DOWN,1.5*HEXSIZE);

        label(pos,"City");
        displace(pos,RIGHT,2*HEXSIZE);
        drawWheat(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawWheat(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawOre(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawOre(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawOre(pos,HEXSIZE/2);
        displace(pos,LEFT,6*HEXSIZE);
        displace(pos,DOWN,1.5*HEXSIZE);

        label(pos,"Development Card");
        displace(pos,DOWN,.75*HEXSIZE);
        displace(pos,RIGHT,2*HEXSIZE);
        drawWheat(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawSheep(pos,HEXSIZE/2);
        displace(pos,RIGHT,HEXSIZE);
        drawOre(pos,HEXSIZE/2);
        displace(pos,LEFT,4*HEXSIZE);
        displace(pos,DOWN,1.5*HEXSIZE);

    }

    int[] dereference(int hexPointer){
        if(hexPointer<ocean.length)
            return ocean[hexPointer];
        else
            return hexes[hexPointer-ocean.length];

    }

    static void displace( double[] pos, double[] direction, double amount){
        pos[0]+=(amount * direction[0]);
        pos[1]+=(amount * direction[1]);
    }
    static double[] newD( double[] pos, double[] direction, 
                                  double amount){
        double[] ret = new double[2];
        ret[0]=pos[0]+(amount * direction[0]);
        ret[1]=pos[1]+(amount * direction[1]);
        return ret;
    }

    static void rotate60( double[] direction ){
        double[] newDirection = new double[2];
        newDirection[0] = COS60*direction[0]-SIN60*direction[1];
        newDirection[1] = SIN60*direction[0]+COS60*direction[1];
        direction[0]=newDirection[0];
        direction[1]=newDirection[1];
    }

}
