(ns com.kotcrab.vis.ui.widget.text-field
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.ui.widget :as widget])
  (:import (clojure.lang ILookup)
           (com.kotcrab.vis.ui.widget VisTextField)))

(defn create
  [{:keys [text-field/text]
    :as opts}]
  (let [actor (-> (proxy [VisTextField ILookup] [(str text)]
                    (valAt [k]
                      (case k
                        :text-field/text (VisTextField/.getText this))))
                  (widget/set-opts! opts))]
    (when-let [tooltip (:tooltip opts)]
      (actor/add-tooltip! actor tooltip))
    actor))
