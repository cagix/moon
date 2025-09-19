(ns com.badlogic.gdx.scenes.scene2d.ui.window
  (:require [gdl.scene2d.actor :as actor]
            [gdl.scene2d.ui.window :as window])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Window)))

(extend-type Window
  window/Ancestor
  (find-ancestor [actor]
    (if-let [parent (actor/parent actor)]
      (if (instance? Window parent)
        parent
        (window/find-ancestor parent))
      (throw (Error. (str "Actor has no parent window " actor))))))
