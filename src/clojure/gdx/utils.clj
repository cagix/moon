(ns clojure.gdx.utils)

(defprotocol Disposable
  (dispose [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose [this]
    (.dispose this)))

(defn disposable? [object]
  (satisfies? Disposable object))
