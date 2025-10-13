(ns cdq.ui.tooltip
  (:require [cdq.ui.stage :as stage]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui.label :as label]
            [clojure.gdx.utils.align :as align]
            [clojure.vis-ui.label :as vis-label]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn add! [actor tooltip-text]
  (tooltip/create {:update-fn (fn [tooltip]
                                (when-not (string? tooltip-text)
                                  (let [actor (tooltip/target tooltip)
                                        ctx (when-let [stage (actor/stage actor)]
                                              (stage/ctx stage))]
                                    (when ctx
                                      (tooltip/set-text! tooltip (tooltip-text ctx))))))
                   :target actor
                   :content (doto (vis-label/create (if (string? tooltip-text) tooltip-text ""))
                              (label/set-alignment! align/center))})
  actor)

(defn remove! [actor]
  (tooltip/remove! actor))
