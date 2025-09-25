(ns com.badlogic.gdx.utils.disposable
  (:require gdl.disposable)
  (:import (com.badlogic.gdx.utils Disposable)))

(extend-type Disposable
  gdl.disposable/Disposable
  (dispose! [this]
    (.dispose this)))
