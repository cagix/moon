(ns clojure.gdx.utils.disposable
  (:require clojure.disposable))

(extend-type com.badlogic.gdx.utils.Disposable
  clojure.disposable/Disposable
  (dispose! [this]
    (.dispose this)))
