
/**
   Wolfram の基本セルオートマトン
   Time-stamp: "2005/07/25 Mon 12:32  hig"
   Saburo Higuchi 2004  http://www.math.ryukoku.ac.jp/~hig/
   Samples: http://sparrow.math.ryukoku.ac.jp/~hig/mobilejava/examples/
*/

// #define DEBUG

// #define DOJA
// #define 1

// 次は, DoJa/1 Builder で禁止されている, #if #endif 間の#define を含みます.
// DoJa/1 Builder を使うときは, // でcomment out し一方だけを残しましょう.

// for V-appli and EZ-appli (1)
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.Random;
// import java.util.Date;

public class CAWolfram256 extends MIDlet {

    // 起動するときに呼ばれるメソッド. 必須.

    public void startApp(){
	CAWolfram256Canvas cc = new CAWolfram256Canvas(this);
	Display.getDisplay(this).setCurrent(cc);
    }

    public void pauseApp(){}

    public void destroyApp(boolean unconditional) throws MIDletStateChangeException {}
    
    // destroyApp は protected なのでラップする
    void terminate(){
	try {
	    destroyApp(false);
	    notifyDestroyed();
	} catch( MIDletStateChangeException e ){}
    }

}

class Rule {

    final int nNeighbors=3; // 近傍の数2 ＋ 自分1
    final int nPatterns=1 << nNeighbors; // 3個のセルの状態の数
    final int nRules=1 <<  (1 << nNeighbors); // ルールの総数

    int rulenum;
    int next [] = new int [nPatterns];
    String [] formula = new String [nPatterns];

    Rule(int rulenum){
	if(rulenum < 0 || rulenum >= nRules ){
	    // へん 例外を投げた方がよい
	}

	this.setRuleNum(rulenum);
    }

    public int getRuleNum(){
	return rulenum;
    }

    public void setRuleNum(int rulenum){
	this.rulenum=rulenum;

	for(int i=0; i < nPatterns ; i++){
	    next[i]= (rulenum >> i ) & 0x1;

	    formula[i]="";
	    for(int j=nNeighbors-1; j>=0; j--){
		formula[i]+=(i & (1<<j))>>j;
	    }
	    formula[i]+=("→" + next[i]);
	}
	return;
    }

    public void calculateNext(Lattice a, int step){

	// 周期境界条件 mod a.getHsize() で書くほうがきれいだけど遅いだろう。
 	a.cell[0][step+1]=next[ a.cell[1][step] 
 			      + 2*a.cell[0][step] 
 			      + 4*a.cell[a.getHsize()-1][step] ];
 	a.cell[a.getHsize()-1][step+1]=next[ a.cell[0][step] 
 			      + 2*a.cell[a.getHsize()-1][step] 
 			      + 4*a.cell[a.getHsize()-2][step] ];

	// bulk
	for(int i=1; i< a.getHsize()-1 ; i++){
	    int tmp=0;
	    for(int j=-1; j<2 ; j++){
		tmp+=a.cell[i-j][step]* (1<<(j+1));
	    }
	    a.cell[i][step+1]=next[tmp];
	}
    }
}

class Lattice {
    int ncellh;			// 横方向の cell の個数
    int ncellv;			// 縦方向の cell の個数
    int [][] cell;
    public final int VALUEZERO=0;
    public final int VALUEONE=1;

    public int getHsize(){
	return ncellh;
    }

    public int getVsize(){
	return ncellv;
    }

    public int getValue(int x, int y){
	return cell[x][y];
    }

    Lattice(int screenWidth, int screenHeight, int cellsize){

	ncellv=screenHeight/cellsize+1;
	ncellh=screenWidth/cellsize+1;

	if( ncellh > 0 && ncellv > 0){
	    cell=new int[ncellh][ncellv];
	} else {
	    // error
	}
    }

    void reset(int seed,int ratio){
	Random r= new Random(seed);

	if( ratio==0 ){
	for(int i=0; i< ncellh; i++){
	    for(int j=0; j< ncellv ; j++){
		cell[i][j]=VALUEZERO;
	    }
	}
	    cell[ncellh/2][0]= VALUEONE;
	    
	} else {
	int threshold=0x00ffffff * ratio;
	for(int i=0; i< ncellh; i++){
	    for(int j=0; j< ncellv ; j++){
		cell[i][j]=VALUEZERO;
	    }
	    if( (r.nextInt() & 0x00ffffff  ) * 100 < threshold ){
		cell[i][0]= VALUEONE;
	    } else {
		cell[i][0]= VALUEZERO;
	    }
	}
	}

    }
}

class CAWolfram256Canvas extends Canvas implements Runnable,
					     CommandListener
{
    CAWolfram256 parent;	// これを呼び出したオブジェクト
    final int fheight=Font.getDefaultFont().getHeight();
    final int fwidth=Font.getDefaultFont().stringWidth("8");

    Rule r=new Rule(184);	// ルールを定めるオブジェクト
    Lattice la;
    int randomseed=(int)System.currentTimeMillis();
    int ratio=40;

    final String [] vname ={"ルール番号", "初期占有率(%)","乱数のシード", "セル幅(dot)", "待ち時間(ms)"};
    int v[] = new int[vname.length];

    // flags
    boolean inanim=false;	// アニメ中であるかどうか

    Thread rt;			// アニメ用スレッド
    int step=0;			// time counter. run() ごとに1ずつ増える.
    int dt=700;		// millisecs  スレッドのスリープ時間

    // グラフィックス    
    Graphics offgr;		// double buffering 用
    Image offimg;		// double buffering 用
    // DoJa の場合, implicit に確保されており, lock(), unlock() で使える
    int cellsize=8;		// cell の一辺のドット数
    int originx;
    int originy;
    final int [] col={( (255<<16) + (255<<8) + (255)), ( (0<<16) + (0<<8) + (255))}; // ZERO, ONE に対応する色

    // Panel/Form
    final String formtitle="ルール";

    final String infotitle="ウォルフラムのセルオートマトン";
    final String infotext
	="http://hig3.net" + "\n" + 
	"(c)2004 hig" + "\n" + 
	"龍谷大学数理情報学科";

    TextField [] bi;

    // コマンド/ソフトキーのラベル
    Command[] ccanvas,cform;
    final String[] cctitle = {"rule", "終了"}; // Canvasの時
    final String[] cftitle = {"実行", "情報"}; // Form/Panelの時

    CAWolfram256Canvas(CAWolfram256 m){

	this.parent=m;

	calculateGeometry();

	// Canvas でのソフトキー/コマンドキーの設定
	ccanvas = new Command[cctitle.length];
	for(int i=0; i < ccanvas.length; i++){
	    ccanvas[i] = new Command(cctitle[i], Command.SCREEN,i);
	    addCommand(ccanvas[i]);
	}
	setCommandListener(this);

	offimg=Image.createImage(getWidth(),getHeight());
	offgr=offimg.getGraphics();

	// スレッド起動
	if( rt == null ){
	    rt = new Thread(this);
	    rt.start();
	}
    }

    void calculateGeometry(){
	originx=0;
	la=new Lattice(getWidth(), getHeight(),cellsize);
	la.reset(randomseed,ratio);
	originy=2*fheight;
    }

    void reset(){
	inanim=false;
	step=0;
	calculateGeometry();
    }

    public void run(){
	while( rt == Thread.currentThread() ){

	    if( inanim ){
		if( step+1 < la.getVsize()-1 ){
		    r.calculateNext(la,step);
		    step++;
		} else {
		    inanim=false;
		}
	    }
		repaint();
		try {
		    Thread.sleep(dt);
		} catch ( InterruptedException e){
		    break;
		}
	}
    }

    void drawSquare(Graphics g,int xcell, int ycell, int val){
	g.setColor(col[val]);
	g.fillRect(originx+xcell*cellsize,originy+ycell*cellsize,cellsize,cellsize);
    }

    public void paint(Graphics g){

	// ダブルバッファリング前処理
	offgr.setColor(( (255<<16) + (255<<8) + (255)));
	offgr.fillRect(0,0,getWidth(),getHeight());// 画面を消す

// 	/* draw color gauge */
// 	for(int i=0; i<recursion.getm(); i++){
// 	    offgr.setColor(col[i]);
// 	    offgr.drawString(""+i, 0+i*fwidth, originy ,Graphics.LEFT|Graphics.BOTTOM);
// 	}

	/* draw a field */
	for(int x=0; x<+la.getHsize(); x++){
	    for(int y=0; y< la.getVsize(); y++){
		drawSquare(offgr, x, y, la.getValue(x,y));;
	    }
	}

    // TODO 説明
        offgr.setColor(0);
// 	offgr.drawString(infotitle,0,fheight ,Graphics.LEFT|Graphics.BOTTOM);
// 	offgr.drawString(recursion.getFormula(),0,0+2*fheight ,Graphics.LEFT|Graphics.BOTTOM);
 	offgr.drawString("実行/停止:選択キー,step:*",0,0+2*fheight ,Graphics.LEFT|Graphics.BOTTOM);
 	offgr.drawString("Wolfram R"+r.getRuleNum(),0,0+1*fheight ,Graphics.LEFT|Graphics.BOTTOM);

	// ダブルバッファリング後処理
	g.drawImage(offimg,0,0,g.LEFT|g.TOP);
    }

    // フォームを表示する.
    public void showPanel(){

	Form p;
	p = new Form(formtitle);

	
  	bi = new TextField[v.length];

	v[0]=r.getRuleNum();
	v[1]=ratio;
	v[2]=randomseed;
	v[3]=cellsize;
	v[4]=dt;

  	for(int j=0; j< v.length; j++){
  	    String tmptitle=vname[j]+"=";
  	    String tmpvalue=""+v[j];
  	    bi[j] = new TextField(tmptitle,tmpvalue,20,TextField.ANY);
	    bi[j].setConstraints(TextField.NUMERIC); // or ANY
  	    p.append(bi[j]);
	    if(j==1){
		p.append(new StringItem("" , "占有率=0→中央に1個"));
	    }
  	}

	    

	cform= new Command [cftitle.length];
	for(int i=0; i < cform.length; i++){
	    cform[i] = new Command(cftitle[i], Command.SCREEN,i);
	    p.addCommand(cform[i]);
	}
	p.setCommandListener(this);
	Display.getDisplay(parent).setCurrent(p);
    }

    void readBoxes(){
  	for(int k=0; k< v.length ; k++){
	    v[k]=Integer.parseInt(bi[k].getString());
	}
    }

    void setParams(){
  	r.setRuleNum(v[0]);
	ratio=v[1];
	if( ratio<0 ){
	    ratio=0;
	} else if (ratio > 100){
	    ratio=100;
	}
	randomseed=v[2];
	cellsize=v[3];
	dt=v[4];
  	reset();
    }

    // キャンバスの時およびフォームの時のコマンドキー
    public void commandAction(Command cx, Displayable s){
	if( cx==ccanvas[0] ){		
		inanim=false;
		showPanel();
	} else if ( cx==ccanvas[1] ){
	    parent.terminate();
	} else if ( cx==cform[0] ){
	    readBoxes();
	    setParams();
	    Display.getDisplay(parent).setCurrent(this);
	} else if ( cx==cform[1] ){
	    String tmp="";

	    tmp+="Rule " + r.getRuleNum() + "\n";
	    for(int i=0; i < 4 ; i++ ){
		tmp+=r.formula[0+2*i]+","+r.formula[1+2*i]+",\n";
	    }
	    tmp+=infotext;

  	    Alert info= new Alert(infotitle,tmp,null,AlertType.INFO);
	    info.setTimeout(Alert.FOREVER);
	    Display.getDisplay(parent).setCurrent(info);
	    reset();
  	}
    }

    // キャンバスの時の一般キー
    protected synchronized void keyPressed(int keyCode){
	int action=getGameAction(keyCode);
	if (action==FIRE){
		inanim=!inanim;
	}  else if ( keyCode==KEY_STAR){
	    // step execution
	    inanim=false;
	    r.calculateNext(la,step);
	    step++;
	}

    }

    protected synchronized void keyReleased(int keycode){
    }

    protected synchronized void keyRepeated(int keycode){
    }

}

// Local Variables:
// mode: java
// compile-command: "make -k CAWolfram256.java im/CAWolfram256.java ez/CAWolfram256.java vf/CAWolfram256.java"
// End:

