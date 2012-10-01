/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/24/11
 * Time: 2:16 PM
 */
package siris.components.editor.gui

import scala.collection.mutable.{Set, Map, SynchronizedMap, HashMap, SynchronizedSet, HashSet}
import siris.components.editor.filesystem.DynamicReloader
import actors.Actor
import java.beans.XMLEncoder
import siris.ontology.SVarDescription
import siris.components.editor.filesystem.ClassFile
import siris.core.entity.typeconversion.ConvertibleTrait
import swing.{GridPanel, Label}
import java.io.{FileWriter, ObjectOutputStream, FileOutputStream, File}
import xml.{Node, PrettyPrinter, Utility, Elem}

/**
 *  Manages the visualisations for svars and cvars
 *
 * @param configFile: The file from/to which the settings are stored/loaded
 * @param editorActor: The corresponding editor component
 */
class TreeNodeVisualisations(configFile: File, editorActor: Actor) {

  private val debug = false

  def makeMap[A, B]: Map[A, B] = {
    new HashMap[A, B] with SynchronizedMap[A, B]
  }

  def makeSet[A]: Set[A] = {
    new HashSet[A] with SynchronizedSet[A]
  }

  private val svarViews: Map[Symbol, DynamicReloader[SVarViewGeneratorBase]] = makeMap[Symbol, DynamicReloader[SVarViewGeneratorBase]]
  private val typeViews: Map[Symbol, Set[DynamicReloader[SVarViewGeneratorBase]]] = makeMap[Symbol, Set[DynamicReloader[SVarViewGeneratorBase]]]

  private val svarSetters: Map[Symbol, DynamicReloader[SVarSetterGeneratorBase]] = makeMap[Symbol, DynamicReloader[SVarSetterGeneratorBase]]
  private val typeSetters: Map[Symbol, Set[DynamicReloader[SVarSetterGeneratorBase]]] = makeMap[Symbol, Set[DynamicReloader[SVarSetterGeneratorBase]]]

  if(configFile.exists) loadConfiguration

  def detailsViewFor(tn: TreeNode): DetailsView = {

    tn match {

      case n: EnRoot => simpleLabelView(n, n.appName _)
      case n: EnEntity => simpleLabelView(n, () => "Entity")
      case n: EnSVarCollection => simpleLabelView(n, () => "SVars")
      case n: EnSVar =>
        val typeInfo = n.svar.containedValueManifest
        //Check manifests
        if(debug)
          if(SVarDescription.apply(Symbol(typeInfo.toString())).head.typeinfo != n.svar.containedValueManifest){
            println("Warning: Manifest difference between " + SVarDescription.apply(Symbol(typeInfo.toString())).head.typeinfo + " and " + n.name.name + "!")
            println("onto: " + SVarDescription.apply(Symbol(typeInfo.toString())).head.typeinfo.erasure.getCanonicalName)
            println("svar: " + n.svar.containedValueManifest.erasure.getCanonicalName)
          }

        if(!typeViews.contains(Symbol(typeInfo.toString))) typeViews += (Symbol(typeInfo.toString) -> makeSet[DynamicReloader[SVarViewGeneratorBase]])
        if(!typeSetters.contains(Symbol(typeInfo.toString))) typeSetters += (Symbol(typeInfo.toString) -> makeSet[DynamicReloader[SVarSetterGeneratorBase]])
        val view = new SVarViewPanel(n.children.head.asInstanceOf[EnSVarValue], n.name, n.svar.containedValueManifest, svarViews, typeViews(Symbol(typeInfo.toString)), editorActor)
        val setter = new SVarSetterPanel(n, n.name, n.svar.containedValueManifest, svarSetters, typeSetters(Symbol(typeInfo.toString)), editorActor)

        new DetailsView{
          val component = new GridPanel(2, 1) {contents += setter.component; contents+= view.component}
          def update(): Unit = {view.update; setter.update}
          val node = n.children.head.asInstanceOf[EnSVarValue]
        }

      case n: EnSVarValue => simpleLabelView(n, () => n.value.toString)
      case n: EnCreateParamSet => simpleLabelView(n, () => "EntityAspect " + n.cps.semantics +  " of " + n.component.name)
      case n: EnCreateParam =>
        //Check manifests
        if(debug)
          if(SVarDescription.apply(Symbol(n.cpb.typedSemantics.typeinfo.toString)).head.typeinfo != n.cpb.containedValueManifest) {
            println("Warning: Manifest difference between " + SVarDescription.apply(Symbol(n.cpb.typedSemantics.typeinfo.toString)).head.sVarIdentifier + " and " + n.cpb.typedSemantics.sVarIdentifier.name + "!")
            println("onto: " + SVarDescription.apply(Symbol(n.cpb.typedSemantics.typeinfo.toString)).head.typeinfo.erasure.getCanonicalName)
            println("svar: " + n.cpb.containedValueManifest.erasure.getCanonicalName)
          }

        if(!typeViews.contains(Symbol(n.cpb.typedSemantics.typeinfo.toString))) typeViews += (Symbol(n.cpb.typedSemantics.typeinfo.toString) -> makeSet[DynamicReloader[SVarViewGeneratorBase]])
        new SVarViewPanel(n.children.head.asInstanceOf[EnCreateParamValue], n.cpb.typedSemantics.sVarIdentifier, n.cpb.containedValueManifest, svarViews, typeViews(Symbol(n.cpb.typedSemantics.typeinfo.toString)), editorActor)

      case n: EnCreateParamValue => simpleLabelView(n, () => n.toString)
      case n => simpleLabelView(n, () => "Unknown")
    }
  }

  private def simpleLabelView(tn: TreeNode, f: () => String) =
    new DetailsView {
      val component = new Label
      def update = {component.text = f()}
      val node = tn
    }

  private def loadConfiguration = {
    println("Loading configuration from: " + configFile.getCanonicalPath)
    val xml = scala.xml.XML.loadFile(configFile)

    //Views
    val typedSVarViews = xml \\ "TypedSvarView"
    for(typedSVarView <- typedSVarViews) {
      val typeInfo = Symbol((typedSVarView \ "typeInfo").text)
      val sVarViewSet = makeSet[DynamicReloader[SVarViewGeneratorBase]]
      val sVarViewElems = (typedSVarView \\ "SVarView")
      for(sVarViewElem <- sVarViewElems) {
        val file = new File((sVarViewElem \ "fileName").text)
        val className = (sVarViewElem \ "className").text

        val dr = new DynamicReloader[SVarViewGeneratorBase] (
          ClassFile(file, className),
          SVarViewPanel.compilerSettings,
          None,
          (svarViewGenBaseOption: Option[SVarViewGeneratorBase]) => {}
        ) {
          override def toString(): String = {
            getCurrentClass match {
              case Some(clazz) => clazz.toString
              case None => "Class loaded with errors"
            }
          }
        }

        sVarViewSet.add(dr)

        val usedForSVars = (sVarViewElem \ "usedForSVars") \ "sVarIdentifier"


        for(useElem <- usedForSVars) {
          val sVarId = Symbol(useElem.text)
          svarViews += sVarId -> dr
        }
      }

      if(!sVarViewSet.isEmpty) typeViews += typeInfo -> sVarViewSet
    }

    //Setters
    val typedSVarSetters = xml \\ "TypedSvarSetter"
    for(typedSVarSetter <- typedSVarSetters) {
      val typeInfo = Symbol((typedSVarSetter \ "typeInfo").text)
      val sVarSetterSet = makeSet[DynamicReloader[SVarSetterGeneratorBase]]
      val sVarSetterElems = (typedSVarSetter \\ "SVarSetter")
      for(sVarSetterElem <- sVarSetterElems) {
        val file = new File((sVarSetterElem \ "fileName").text)
        val className = (sVarSetterElem \ "className").text

        val dr = new DynamicReloader[SVarSetterGeneratorBase] (
          ClassFile(file, className),
          SVarSetterPanel.compilerSettings,
          None,
          (svarViewGenBaseOption: Option[SVarSetterGeneratorBase]) => {}
        ) {
          override def toString(): String = {
            getCurrentClass match {
              case Some(clazz) => clazz.toString
              case None => "Class loaded with errors"
            }
          }
        }

        sVarSetterSet.add(dr)

        val usedForSVars = (sVarSetterElem \ "usedForSVars") \ "sVarIdentifier"


        for(useElem <- usedForSVars) {
          val sVarId = Symbol(useElem.text)
          svarSetters += sVarId -> dr
        }
      }

      if(!sVarSetterSet.isEmpty) typeSetters += typeInfo -> sVarSetterSet
    }

//    println(Utility.toXML(x = xml, preserveWhitespace = true).toString)
//    println(xml.toString)
//    val p = new PrettyPrinter(1000,2)
//    println(p.format(xml))

  }

  def saveConfiguration() = {
    println("Saving configuration to: " + configFile.getCanonicalPath)

    //Views
    val tvs = for(typeView <- typeViews.filter(_._2.nonEmpty).toSeq.sortWith(_._1.name < _._1.name)) yield {
      val typeInfo = <typeInfo>{typeView._1.name}</typeInfo>
      val views = for(view <- typeView._2.toSeq.sortWith(_.classFile.className < _.classFile.className)) yield {
        val fileName = <fileName>{new File(System.getProperty("user.dir")).toURI.relativize(view.classFile.file.toURI).getPath}</fileName>
        val className = <className>{view.classFile.className}</className>
        val usedToVisualizeSet = svarViews.filter((symbolDrTuple) => {symbolDrTuple._2 == view})
        val usedToVisualize = for(symbolDrTuple <- usedToVisualizeSet.toSeq.sortWith(_._1.name < _._1.name)) yield {
          <sVarIdentifier>{symbolDrTuple._1.name}</sVarIdentifier>
        }
        val usedForSVars = new Elem(null, "usedForSVars", null, scala.xml.TopScope, usedToVisualize.toSeq:_*)
        new Elem(null, "SVarView", null, scala.xml.TopScope, fileName, className, usedForSVars)
      }
      val viewsElem = new Elem(null, "SVarViews", null, scala.xml.TopScope, views.toSeq:_*)
      new Elem(null, "TypedSvarView", null, scala.xml.TopScope, typeInfo, viewsElem)
    }
    val typedSvarViews = new Elem(null, "TypedSvarViews", null, scala.xml.TopScope, tvs.toSeq:_*)

    //Setters
    val tss = for(typeSetter <- typeSetters.filter(_._2.nonEmpty).toSeq.sortWith(_._1.name < _._1.name)) yield {
      val typeInfo = <typeInfo>{typeSetter._1.name}</typeInfo>
      val setters = for(setter <- typeSetter._2.toSeq.sortWith(_.classFile.className < _.classFile.className)) yield {
        val fileName = <fileName>{new File(System.getProperty("user.dir")).toURI.relativize(setter.classFile.file.toURI).getPath}</fileName>
        val className = <className>{setter.classFile.className}</className>
        val usedToSetSet = svarSetters.filter((symbolDrTuple) => {symbolDrTuple._2 == setter})
        val usedToSet = for(symbolDrTuple <- usedToSetSet.toSeq.sortWith(_._1.name < _._1.name)) yield {
          <sVarIdentifier>{symbolDrTuple._1.name}</sVarIdentifier>
        }
        val usedForSVars = new Elem(null, "usedForSVars", null, scala.xml.TopScope, usedToSet.toSeq:_*)
        new Elem(null, "SVarSetter", null, scala.xml.TopScope, fileName, className, usedForSVars)
      }
      val viewsElem = new Elem(null, "SVarSetters", null, scala.xml.TopScope, setters.toSeq:_*)
      new Elem(null, "TypedSvarSetter", null, scala.xml.TopScope, typeInfo, viewsElem)
    }
    val typedSvarSetters = new Elem(null, "TypedSvarSetters", null, scala.xml.TopScope, tss.toSeq:_*)

    val xml = new Elem(null, "EditorConfig", null, scala.xml.TopScope, typedSvarViews, typedSvarSetters)

    val fw = new FileWriter(configFile)
    //The conversion to String and back is a quick and dirty way to deal with the "null"s in the
    //above Elem constructors, that lead to a NullPointerException in the PrittyPrinter but not in the toString method of Node
    fw.write((new PrettyPrinter(1000,2)).format(scala.xml.XML.loadString(xml.toString)))
    fw.close()
  }

}
