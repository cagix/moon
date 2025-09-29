(ns com.badlogic.gdx.utils.disposable)

(defprotocol Disposable
  (dispose! [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [this]
    (.dispose this)))
