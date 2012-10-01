package siris.components.renderer.jvr.types

import siris.ontology.{SVarDescription, Symbols}


object HeadTransform extends SVarDescription[de.bht.jvr.core.Transform, simplex3d.math.floatm.renamed.Mat4x4]( siris.ontology.types.HeadTransform as Symbols.headTransform createdBy(new de.bht.jvr.core.Transform) )

object Interactions extends SVarDescription( siris.ontology.types.AnyList as Symbols.interactions )

object Scale extends SVarDescription[de.bht.jvr.core.Transform, simplex3d.math.floatm.renamed.Mat4x4]( siris.ontology.types.Scale as Symbols.scale createdBy(new de.bht.jvr.core.Transform) )
object ShowHeightMap extends SVarDescription( siris.ontology.types.Boolean as Symbols.showHeightMap )

object Texture extends SVarDescription(siris.ontology.types.Texture createdBy new de.bht.jvr.core.Texture2D(0,0,0,0))
object Transformation extends SVarDescription[de.bht.jvr.core.Transform, simplex3d.math.floatm.renamed.Mat4x4]( siris.ontology.types.HeadTransform as Symbols.transformation createdBy(new de.bht.jvr.core.Transform) )

object ViewPlatform extends SVarDescription[de.bht.jvr.core.Transform, simplex3d.math.floatm.renamed.Mat4x4]( siris.ontology.types.HeadTransform as Symbols.viewPlatform createdBy(new de.bht.jvr.core.Transform) )

object DiffuseColor extends SVarDescription[de.bht.jvr.util.Color, java.awt.Color]( siris.ontology.types.DiffuseColor createdBy(new de.bht.jvr.util.Color(0,0,0)))
object SpecularColor extends SVarDescription[de.bht.jvr.util.Color, java.awt.Color]( siris.ontology.types.SpecularColor createdBy(new de.bht.jvr.util.Color(0,0,0)))