========
ROAD MAP
========


After releasing version 1.0, the author of yaidom tried to write a blog post, showing the purpose and strength of
the different query API traits. That turned out to be difficult, because the query API traits are not sufficiently
orthogonal, depending on inheritance too much. For example, in yaidom 1.0 we cannot abstract over indexed and docaware
elements. Moreover, some query API traits are far more useful than others.

In particular, the ParentElemApi trait is by far the most important one, but the ElemApi trait offers nothing fundamental.
(It makes sense to rename ParentElemApi to ElemApi, thus effectively removing the old ElemApi API.) The PathAwareElemApi
trait offers too little, compared to an ad-hoc combination of ElemApi (after the renaming) and new trait HasPath, thus
modeling indexed and docaware elements. Most element implementations can then be seen as the combination of ElemApi
(after the renaming) and several "capabilities" (such as knowing about ENames, about QNames, about text content, etc.),
at least for the query API (as opposed to update/transformation API).

Fortunately, most element implementations make sense as they are (for the most part), and it is just the underlying
query API traits that need an overhaul. Given that the query API traits rarely occur in yaidom client code, the impact
of such an overhaul can be rather limited. Moreover, this internal overhaul is not fundamental enough to regard this
as part of "yaidom 2", rather than an evolution of "yaidom 1".

While cleaning up the query API traits, it also makes sense to split the root package of yaidom into 3 sub-packages:
"core" (with concepts such as QName, EName, Scope etc.), "queryapi" (with the query API traits) and "defaultelem" (with
the default Elem implementation, along with ElemBuilder). This would impact a lot of import statements in yaidom
client code, and can be facilitated by aliases in the root yaidom package that can gradually be deprecated and removed.

This leads to the road map below (shortly after 1.0).


1.1
===

Changes:

* Splitting the root package into 3 sub-packages (as mentioned above)
* Hiding this split to the yaidom user, by aliases in the root package
* Rework the query API traits, which is a breaking change
* Yet leave the net query API offered by the different element implementations as much as possible the same.


1.2
===

Changes:

* Deprecate the aliases in the root package, giving yaidom users the time to upgrade to version 1.3


1.3
===

Changes:

* Remove the deprecated aliases in the root package, resulting in a meaner and cleaner yaidom
