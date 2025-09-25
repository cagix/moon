(ns com.badlogic.gdx.scenes.scene2d.ui.label
  (:require [gdl.scene2d.ui.label])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Label)))

(extend-type Label
  gdl.scene2d.ui.label/Label
  (set-text! [label text]
    (.setText label (str text))))
