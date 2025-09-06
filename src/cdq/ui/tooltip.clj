(ns cdq.ui.tooltip
  (:require [cdq.ui.stage :as stage]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.vis-ui.tooltip :as tooltip]))

(defn add!
  [actor tooltip-text]
  (tooltip/add! actor
                tooltip-text
                (fn [tooltip]
                  (when-not (string? tooltip-text)
                    (let [actor (tooltip/get-target tooltip)
                          ; acturs might be initialized without a stage yet so we do when-let
                          ; FIXME double when-let
                          ctx (when-let [stage (actor/get-stage actor)]
                                (stage/get-ctx stage))]
                      (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                        (tooltip/set-text! tooltip (str (tooltip-text ctx))))))))
  actor)

(def remove! tooltip/remove!)
