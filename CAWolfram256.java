/**
   Wolfram の基本セルオートマトン
   Time-stamp: "2004/12/17 Fri 18:55  hig"
   Saburo Higuchi 2004  http://www.math.ryukoku.ac.jp/~hig/
   Samples: http://sparrow.math.ryukoku.ac.jp/~hig/mobilejava/examples/
*/

// #define DEBUG

#if FALSE
// このファイルに #if などの directive が残っている場合,
// cpp または DoJa/MIDP Builder のプリプロセッサに処理させてください.
// その際, 次のどちらか一方を定義してください.
#endif

// #define DOJA
// #define MIDP

#if FALSE
// cpp の場合, 両方comment out しておいて, cpp -DDOJA などと選択することもできます. 
// 樋口のMakefileではそうなってます.
#endif

// 次は, DoJa/MIDP Builder で禁止されている, #if #endif 間の#define を含みます.
// DoJa/MIDP Builder を使うときは, // でcomment out し一方だけを残しましょう.
#if DOJA

 #define MYALIGN +0
 #define MYAP IApplication
 #define MYBLACK Graphics.getColorOfName(Graphics.BLACK)
 #define MYWHITE Graphics.getColorOfName(Graphics.WHITE)
 #define MYBLUE Graphics.getColorOfName(Graphics.BLUE)
 #define MYRED Graphics.getColorOfName(Graphics.RED)
 #define MYGREEN Graphics.getColorOfName(Graphics.GREEN)
 #define MYYELLOW Graphics.getColorOfName(Graphics.YELLOW)
 #define MYMAGENTA Graphics.getColorOfName(Graphics.MAROON)
 #define MYCYAN Graphics.getColorOfName(Graphics.AQUA)
 #define MYDIALOG Dialog
 #define MYLABEL Label
 #define MYIMAGELABEL ImageLabel
 #define MYADD add
 #define MYCHOICE ListBox
 #define MYPANEL Panel
 #define MYNULL +""
 #define MYGETTEXT getText
 #define MYSETTEXT setText
 #define MYCORP +

 #define MYINPUTBOXTITLE Label
 #define MYINPUTBOX TextBox

#elif MIDP

#define MYALIGN ,Graphics.LEFT|Graphics.BOTTOM
#define MYAP MIDlet
#define MYBLACK 0
#define MYWHITE ( (255<<16) + (255<<8) + (255))
#define MYBLUE  ( (0<<16) + (0<<8) + (255))
#define MYRED   ( (255<<16) + (0<<8) + (0))
#define MYGREEN ( (0<<16) + (255<<8) + (0))
#define MYYELLOW ( (255<<16) + (255<<8) + (0))
#define MYMAGENTA ( (255<<16) + (0<<8) + (255))
#define MYCYAN ( (0<<16) + (255<<8) + (255))
#define MYDIALOG Alert
#define MYLABEL StringItem
#define MYIMAGELABEL ImageItem
#define MYADD append
#define MYCHOICE ChoiceGroup
#define MYTEXTBOX TextField
#define MYPANEL Form
#define MYNULL ,null
#define MYGETTEXT getString
#define MYSETTEXT setString
#define MYCORP ,

#define MYINPUTBOX TextField
#define MYINPUTBOXTITLE not_necessary

#endif


#if DOJA
// for DoCoMo i-appli
import com.nttdocomo.ui.*;
import com.nttdocomo.util.*;
#elif MIDP
// for V-appli and EZ-appli (MIDP)
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
#endif
import java.util.Random;
// import java.util.Date;

public class CAWolfram256 extends MYAP {

    // 起動するときに呼ばれるメソッド. 必須.

#if DOJA
    public void start(){
	CAWolfram256Canvas cc=new CAWolfram256Canvas(this);
	Display.setCurrent(cc);
    }

//      /** メーラなどから戻るときに呼ばれるメソッド. オーバーライドは必須でない.*/
//      public void resume(){
//  	// 何とか
//      }


//      /** 終了するときに呼ばれるメソッド. オーバーライドは必須でない.*/
//      public void terminate(){
//  	// 何とか
//      }


#elif MIDP
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
#endif

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
#if DOJA
					     SoftKeyListener,ComponentListener 
#elif MIDP
					     CommandListener
#endif
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
#if DOJA
#elif MIDP    
    Image offimg;		// double buffering 用
    // DoJa の場合, implicit に確保されており, lock(), unlock() で使える
#endif
    int cellsize=8;		// cell の一辺のドット数
    int originx;
    int originy;
    final int [] col={MYWHITE, MYBLUE}; // ZERO, ONE に対応する色

    // Panel/Form
    final String formtitle="ルール";

    final String infotitle="ウォルフラムのセルオートマトン";
    final String infotext
	="http://hig3.net" + "\n" + 
	"(c)2004 hig" + "\n" + 
	"龍谷大学数理情報学科";


#if DEBUG
    int w,h;
#endif    

    MYINPUTBOX [] bi;
#if DOJA
    MYINPUTBOXTITLE [] bititle;       // bi のタイトル
    // MIDP では TextField の Label を使えばよい.
#elif MIDP
#endif

    // コマンド/ソフトキーのラベル
#if DOJA
#elif MIDP
    Command[] ccanvas,cform;
#endif
    final String[] cctitle = {"rule", "終了"}; // Canvasの時
    final String[] cftitle = {"実行", "情報"}; // Form/Panelの時

    CAWolfram256Canvas(CAWolfram256 m){

	this.parent=m;
#if DEBUG
	w=getWidth();
	h=getHeight();
#endif

	calculateGeometry();

	// Canvas でのソフトキー/コマンドキーの設定
#if  DOJA	
	if( cctitle.length > 0 ){
	    setSoftLabel(Frame.SOFT_KEY_1,cctitle[0]);
	    if( cctitle.length > 1 ){
		setSoftLabel(Frame.SOFT_KEY_2,cctitle[1]);
	    }
	}
#elif MIDP
	ccanvas = new Command[cctitle.length];
	for(int i=0; i < ccanvas.length; i++){
	    ccanvas[i] = new Command(cctitle[i], Command.SCREEN,i);
	    addCommand(ccanvas[i]);
	}
	setCommandListener(this);
#endif

#if DOJA
#elif MIDP
	offimg=Image.createImage(getWidth(),getHeight());
	offgr=offimg.getGraphics();
#endif


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
#if DOJA
	offgr=g;		// これは MIDP と記述をそろえるためだけ
	offgr.lock();		//画面をいったん固定(ダブルバッファリング)
	offgr.clearRect(0,0,getWidth(),getHeight());// 画面を消す
#elif MIDP
	offgr.setColor(MYWHITE);
	offgr.fillRect(0,0,getWidth(),getHeight());// 画面を消す
#endif


#if DEBUG
	offgr.setColor(MYBLACK);
	offgr.drawString(""+r.next[0]+" "+r.next[1]+" "+r.next[2]+" "+r.next[3],originx,30 MYALIGN);
	offgr.drawString(""+r.next[4]+" "+r.next[5]+" "+r.next[6]+" "+r.next[7],originx,40 MYALIGN);
	offgr.drawString(""+ inanim + " " + step,originx,50 MYALIGN);
#endif

// 	/* draw color gauge */
// 	for(int i=0; i<recursion.getm(); i++){
// 	    offgr.setColor(col[i]);
// 	    offgr.drawString(""+i, 0+i*fwidth, originy MYALIGN);
// 	}

	/* draw a field */
	for(int x=0; x<+la.getHsize(); x++){
	    for(int y=0; y< la.getVsize(); y++){
		drawSquare(offgr, x, y, la.getValue(x,y));;
	    }
	}

    // TODO 説明
        offgr.setColor(MYBLACK);
// 	offgr.drawString(infotitle,0,fheight MYALIGN);
// 	offgr.drawString(recursion.getFormula(),0,0+2*fheight MYALIGN);
 	offgr.drawString("実行/停止:選択キー,step:*",0,0+2*fheight MYALIGN);
 	offgr.drawString("Wolfram R"+r.getRuleNum(),0,0+1*fheight MYALIGN);

	// ダブルバッファリング後処理
#if DOJA
	offgr.unlock(true);		
#elif MIDP
	g.drawImage(offimg,0,0,g.LEFT|g.TOP);
#endif
    }

    // フォームを表示する.
    public void showPanel(){

	MYPANEL p;
#if DOJA	
	p =  new MYPANEL();
	p.setTitle(formtitle);
#elif MIDP
	p = new MYPANEL(formtitle);
#endif

	
  	bi = new MYINPUTBOX[v.length];
#if DOJA
  	bititle = new MYINPUTBOXTITLE[ v.length ];
#elif MIDP
#endif


	v[0]=r.getRuleNum();
	v[1]=ratio;
	v[2]=randomseed;
	v[3]=cellsize;
	v[4]=dt;

  	for(int j=0; j< v.length; j++){
  	    String tmptitle=vname[j]+"=";
  	    String tmpvalue=""+v[j];
#if DOJA
  	    bititle[j] = new MYINPUTBOXTITLE(tmptitle);
  	    bi[j] = new MYINPUTBOX(tmpvalue,4,1,TextBox.DISPLAY_ANY);
  	    bi[j].setEditable(true);
  	    bi[j].setInputMode(TextBox.NUMBER);
  	    p.MYADD(bititle[j]);
  	    p.MYADD(bi[j]);


	    if(j==1){
  	    MYLABEL separator=new MYLABEL("");
  	    separator.setSize(p.getWidth(),1);
  	    p.MYADD(separator);
		p.MYADD(new MYLABEL("" MYCORP "占有率=0→中央に1個"));
	    }

  	    MYLABEL separator=new MYLABEL("");
  	    separator.setSize(p.getWidth(),1);
  	    p.MYADD(separator);

  #elif MIDP
  	    bi[j] = new MYINPUTBOX(tmptitle,tmpvalue,20,MYTEXTBOX.ANY);
	    bi[j].setConstraints(TextField.NUMERIC); // or ANY
  	    p.MYADD(bi[j]);
	    if(j==1){
		p.MYADD(new MYLABEL("" MYCORP "占有率=0→中央に1個"));
	    }
  #endif
  	}

	    

#if DOJA
	if( cftitle.length > 0 ){
	    p.setSoftLabel(Frame.SOFT_KEY_1,cftitle[0]);
	    if( cftitle.length > 1 ){
		p.setSoftLabel(Frame.SOFT_KEY_2,cftitle[1]); 
	    }
	}
	p.setSoftKeyListener(this);
	p.setComponentListener(this);
	Display.setCurrent(p);
#elif MIDP
	cform= new Command [cftitle.length];
	for(int i=0; i < cform.length; i++){
	    cform[i] = new Command(cftitle[i], Command.SCREEN,i);
	    p.addCommand(cform[i]);
	}
	p.setCommandListener(this);
	Display.getDisplay(parent).setCurrent(p);
#endif
    }

    void readBoxes(){
  	for(int k=0; k< v.length ; k++){
	    v[k]=Integer.parseInt(bi[k].MYGETTEXT());
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

#if DOJA
    // パネルの時
    public void componentAction(Component source,  int type, int param){
	if ( type==TEXT_CHANGED /* && source==bi[something] */){
	    readBoxes();
	}
    }

    public void softKeyPressed(int key){
	if( key==Frame.SOFT_KEY_1 ){
	    calculateGeometry();
	    setParams();
	    Display.setCurrent(this);
	} else if ( key==Frame.SOFT_KEY_2){
	    Dialog info= new Dialog(Dialog.DIALOG_INFO,infotitle);
	    String tmp="";

	    tmp+="Rule " + r.getRuleNum() + "\n";
	    for(int i=0; i < 4 ; i++ ){
		tmp+=r.formula[0+2*i]+","+r.formula[1+2*i]+",\n";
	    }
	    tmp+=infotext;
	    info.setText(tmp);
	    info.show();
	}

    }

    public void softKeyReleased(int key){
    }


    // キャンバスの時
    public void processEvent(int type, int param){
	if( type==Display.KEY_PRESSED_EVENT ){
	    switch( param ){
	    case Display.KEY_SELECT:
		inanim=!inanim;
		break;
	    case Display.KEY_ASTERISK:
		// step execution
		inanim=false;
		r.calculateNext(la,step);
		step++;
		break;
	    case Display.KEY_SOFT1:
		inanim=false;
		showPanel();
		break;
	    case Display.KEY_SOFT2:
		parent.terminate();
	    default:
	    }
	} 
	    
    }
#elif MIDP
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
#endif

}



// Local Variables:
// mode: java
// compile-command: "make -k CAWolfram256.java im/CAWolfram256.java ez/CAWolfram256.java vf/CAWolfram256.java"
// End:


