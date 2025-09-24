(ns clojure.disposable
  "Interface for disposable resources.")

(defprotocol Disposable
  (dispose! [_] "Releases all resources of this object."))
