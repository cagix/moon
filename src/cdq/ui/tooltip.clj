(ns cdq.ui.tooltip
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [clojure.scene2d.stage :as stage]
            [com.badlogic.gdx.utils.align :as align]
            [com.kotcrab.vis-ui.widget.tooltip :as tooltip]
            [com.kotcrab.vis-ui.widget.vis-label :as vis-label])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn add! [actor tooltip-text]
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

(defn remove! [actor]
  (tooltip/remove! actor))
