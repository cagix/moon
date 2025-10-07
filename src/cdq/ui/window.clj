(ns cdq.ui.window
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Window)))

(defn find-ancestor
  "Finds the ancestor window of actor, otherwise throws an error if none of recursively searched parents of actors is a window actor."
  [actor]
  (if-let [parent (Actor/.getParent actor)]
    (if (instance? Window parent)
      parent
      (find-ancestor parent))
    (throw (Error. (str "Actor has no parent window " actor)))))
