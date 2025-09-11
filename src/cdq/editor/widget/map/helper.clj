(ns cdq.editor.widget.map.helper
  (:require [cdq.editor-window :as editor-window]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.vis-ui.separator :as separator]))

(defn k->label-text [k]
  (name k) ;(str "[GRAY]:" (namespace k) "[]/" (name k))
  )

(defn component-row
  [{:keys [editor-widget
           display-remove-component-button?
           k
           table
           label-text]}]
  [{:actor {:actor/type :actor.type/table
            :cell-defaults {:pad 2}
            :rows [[{:actor (when display-remove-component-button?
                              {:actor/type :actor.type/text-button
                               :text "-"
                               :on-clicked (fn [_actor ctx]
                                             (actor/remove! (utils/find-first (fn [actor]
                                                                                (and (actor/user-object actor)
                                                                                     (= k ((actor/user-object actor) 0))))
                                                                              (group/children table)))
                                             (editor-window/rebuild! ctx))})
                     :left? true}
                    {:actor {:actor/type :actor.type/label
                             :label/text label-text}}]]}
    :right? true}
   {:actor (separator/vertical)
    :pad-top 2
    :pad-bottom 2
    :fill-y? true
    :expand-y? true}
   {:actor editor-widget
    :left? true}])
