(ns com.badlogic.gdx.scenes.scene2d.ui.window
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [gdl.scene2d.ui.window :as window])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Window)))

(extend-type Actor
  window/Ancestor
  (find-ancestor [actor]
    (if-let [parent (actor/parent actor)]
      (if (instance? Window parent)
        parent
        (window/find-ancestor parent))
      (throw (Error. (str "Actor has no parent window " actor))))))
