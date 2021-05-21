
// Run amm in scripts folder
// In amm session, use command "import $exec.eu.cdevreeze.yaidom.scripts.InitYaidomSession"

// Taking yaidom version 1.13.0

import $ivy.`eu.cdevreeze.yaidom::yaidom:1.13.0`

// Imports that (must) remain available after this initialization script

import java.net.URI
import java.io._
import scala.collection.immutable

import eu.cdevreeze.yaidom.core._
import eu.cdevreeze.yaidom._

// Easy creation of ENames and QNames

object ENameUtil {

  implicit class ToEName(val s: String) {

    /**
     * Returns the EName corresponding to the given QName as string, using the implicitly passed Scope.
     */
    def en(implicit qnameProvider: QNameProvider, enameProvider: ENameProvider, scope: Scope) = {
      val qname = qnameProvider.parseQName(s)
      val ename = scope.resolveQNameOption(qname)(enameProvider).get
      ename
    }

    /**
     * Returns the EName corresponding to the given QName as string, using the implicitly passed Scope,
     * but without default namespace. Use this method to get attribute ENames.
     */
    def an(implicit qnameProvider: QNameProvider, enameProvider: ENameProvider, scope: Scope) = {
      val qname = qnameProvider.parseQName(s)
      val ename = scope.withoutDefaultNamespace.resolveQNameOption(qname)(enameProvider).get
      ename
    }
  }
}

import ENameUtil._

ENameProvider.globalENameProvider.become(jvm.CaffeineENameProvider.fromMaximumCacheSize(5000))
QNameProvider.globalQNameProvider.become(jvm.CaffeineQNameProvider.fromMaximumCacheSize(5000))

// Default parser and printer, without any configuration (not even setting document URI)

val defaultParser = parse.DocumentParserUsingStax.newInstance()

val defaultPrinter = print.DocumentPrinterUsingDom.newInstance()

// Now the REPL has been set up for ad-hoc yaidom querying and transformations
// Do not forget to provide an implicit Scope if we want to create ENames with the "en" or "an" postfix operator!
