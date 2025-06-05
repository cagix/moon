(ns gdl.utils.disposable)

(defprotocol Disposable
  (dispose! [_]
            "Releases all resources of the object."))
