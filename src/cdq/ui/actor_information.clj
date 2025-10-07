(ns cdq.ui.actor-information
  (:require [cdq.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Label
                                               Window)))

(defn- inventory-cell-with-item? [actor]
  (and (Actor/.getParent actor)
       (= "inventory-cell" (Actor/.getName (Actor/.getParent actor)))
       (Actor/.getUserObject (Actor/.getParent actor))))

; FIXME does not work
(defn- window-title-bar?
  "Returns true if the actor is a window title bar."
  [actor]
  (when (instance? Label actor)
    (when-let [p (Actor/.getParent actor)]
      (when-let [p (Actor/.getParent p)]
        (and (instance? Window actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(comment
 ; maybe use this?
 (isa? (class actor) Button)
 )

(defn- button?
  "Returns true if the actor or its parent is a button."
  [actor]
  (or (button-class? actor)
      (and (Actor/.getParent actor)
           (button-class? (Actor/.getParent actor)))))

(extend-type cdq.ui.Stage
  ui/ActorInformation
  (actor-information [_ actor]
    (let [inventory-slot (inventory-cell-with-item? actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window-title-bar? actor) [:mouseover-actor/window-title-bar]
       (button?           actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified]))))
