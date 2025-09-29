(ns clojure.scene2d.vis-ui.text-field
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.ui.widget :as widget]
            [com.kotcrab.vis.ui.widget.vis-text-field :as vis-text-field]))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (vis-text-field/create text)
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (actor/add-tooltip! actor tooltip))
    actor))
