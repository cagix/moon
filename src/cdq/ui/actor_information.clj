(ns cdq.ui.actor-information
  (:require [cdq.ui :as ui]
            [clojure.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button
                                               Label
                                               Window)))

(defn- inventory-cell-with-item? [actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/get-name (actor/parent actor)))
       (actor/user-object (actor/parent actor))))

; FIXME does not work
(defn- window-title-bar?
  "Returns true if the actor is a window title bar."
  [actor]
  (when (instance? Label actor)
    (when-let [p (actor/parent actor)]
      (when-let [p (actor/parent p)]
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
      (and (actor/parent actor)
           (button-class? (actor/parent actor)))))

(extend-type com.badlogic.gdx.scenes.scene2d.CtxStage
  ui/ActorInformation
  (actor-information [_ actor]
    (let [inventory-slot (inventory-cell-with-item? actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window-title-bar? actor) [:mouseover-actor/window-title-bar]
       (button?           actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified]))))
