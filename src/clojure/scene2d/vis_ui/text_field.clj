(ns clojure.scene2d.vis-ui.text-field
  (:require [cdq.ui.tooltip :as tooltip]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [clojure.scene2d.widget :as widget]
            [clojure.gdx.vis-ui.widget.vis-text-field :as vis-text-field]))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (vis-text-field/create text)
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (tooltip/add! actor tooltip))
    actor))
