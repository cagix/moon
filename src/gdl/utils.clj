(ns gdl.utils)

(defprotocol Disposable
  (dispose [obj] "Release all resources of the object."))
