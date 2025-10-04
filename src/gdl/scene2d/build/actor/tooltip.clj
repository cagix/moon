(ns gdl.scene2d.build.actor.tooltip
  (:require [com.badlogic.gdx.utils.align :as align]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip]
            [com.kotcrab.vis.ui.widget.vis-label :as vis-label]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(extend-type Actor
  actor/Tooltip
  (add-tooltip! [actor tooltip-text]
    (tooltip/create {:update-fn (fn [tooltip]
                                  (when-not (string? tooltip-text)
                                    (let [actor (tooltip/target tooltip)
                                          ctx (when-let [stage (actor/get-stage actor)]
                                                (stage/get-ctx stage))]
                                      (when ctx
                                        (tooltip/set-text! tooltip (tooltip-text ctx))))))
                     :target actor
                     :content (doto (vis-label/create (if (string? tooltip-text)
                                                        tooltip-text
                                                        ""))
                                (.setAlignment (align/k->value :center)))})
    actor)

  (remove-tooltip! [actor]
    (tooltip/remove! actor)))
