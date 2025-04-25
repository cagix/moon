(ns cdq.utils)

(defprotocol Disposable
  (dispose! [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [this]
    (.dispose this)))
