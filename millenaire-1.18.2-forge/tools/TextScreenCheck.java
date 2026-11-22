package org.millenaire.client.gui.text;
import net.minecraft.SharedConstants;import net.minecraft.server.Bootstrap;import net.minecraft.client.gui.components.Button;import net.minecraft.client.gui.Font;import net.minecraft.resources.ResourceLocation;import org.millenaire.client.book.*;
public class TextScreenCheck {
 static class TestFont extends Font {
  TestFont(){super(id->null);}
  @Override public int width(String value){return value.length()*6;}
  @Override public String plainSubstrByWidth(String text,int width){return text.substring(0,Math.min(text.length(),Math.max(0,width/6)));}
  @Override public String plainSubstrByWidth(String text,int width,boolean tail){int length=Math.min(text.length(),Math.max(0,width/6));return tail?text.substring(text.length()-length):text.substring(0,length);}
 }
 static class TestScreen extends GuiText {
  int pressed=-1,changes=0;boolean closed=false;
  TestScreen(){width=500;height=400;font=new TestFont();bookManager=new BookManager(256,220,160,240,new FontRendererGUIWrapper(this));initData();}
  public void initData(){textBook=new TextBook();var p1=new TextPage();p1.addLine(new TextLine(new MillGuiButton("first",11)));textBook.addPage(p1);var p2=new TextPage();p2.addLine(new TextLine(new MillGuiButton("second",12)));textBook.addPage(p2);}
  protected void customDrawBackground(int x,int y,float ticks){}protected void customDrawScreen(int x,int y,float ticks){}
  public ResourceLocation getPNGPath(){return new ResourceLocation("millenaire","test");}
  protected void onButtonPressed(Button button){pressed=buttonId(button);}
  protected void handleTextFieldPress(MillGuiTextField field){changes++;}
  public void onClose(){closed=true;}
 }
 public static void main(String[] args){
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  var a=new TestScreen();var b=new TestScreen();a.buttonPagination();b.buttonPagination();
  if(a.children().size()!=1 || a.renderables.size()!=1)throw new AssertionError("Button missing from event/render lists");
  a.buttons.get(0).onPress();if(a.pressed!=11 || b.pressed!=-1)throw new AssertionError("Wrong action owner");
  a.setFocused(a.buttons.get(0));a.incrementPage();if(a.pageNum!=1 || a.children().size()!=1 || a.getFocused()!=null)throw new AssertionError("Pagination retained stale widgets/focus");
  a.buttons.get(0).onPress();if(a.pressed!=12)throw new AssertionError("Wrong second-page action");
  a.incrementPage();if(a.pageNum!=1)throw new AssertionError("Exceeded last page");a.decrementPage();a.decrementPage();if(a.pageNum!=0)throw new AssertionError("Negative page");
  a.pageNum=99;a.buttonPagination();if(a.pageNum!=1)throw new AssertionError("Outdated page not clamped");
  var book=new TextBook();var page=new TextPage();var field=new GuiText.MillGuiTextField(1,new TestFont(),0,0,90,20,"test");field.setMaxLength(8);field.setValue("old");page.addLine(new TextLine(field));book.addPage(page);a.textBook=book;a.pageNum=0;a.buttonPagination();
  var actual=a.textFields.get(0);if(!actual.getValue().equals("old") || actual.getMaxLength()!=8 || !a.children().contains(actual) || !a.renderables.contains(actual))throw new AssertionError("Text field registration/state");
  a.setFocused(actual);actual.setFocus(true);if(!a.charTyped('x',0) || !actual.getValue().contains("x") || a.changes!=1)throw new AssertionError("Text input/responder");
  actual.setValue("123456789");if(actual.getValue().length()!=8)throw new AssertionError("Length limit lost");
  a.keyPressed(256,0,0);if(!a.closed)throw new AssertionError("Escape did not invoke screen close");
  a.textBook=new TextBook();a.pageNum=8;a.buttonPagination();if(a.pageNum!=0 || !a.children().isEmpty())throw new AssertionError("Empty page handling");
  System.out.println("PASS: owning-screen actions, widget registration, pagination/clamping, stale focus cleanup, text state/limits, character input and responder, Escape; service/font fixtures, no graphical render or server action test.");
 }
}
