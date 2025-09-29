(ns gdl.utils.disposable
  "Interface for disposable resources.")

(defprotocol Disposable
  (dispose! [_]
            "Releases all resources of this object."))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [obj]
    (.dispose obj)))
