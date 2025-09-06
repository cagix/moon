(ns cdq.dev.data-view
  (:require [cdq.ui.text-button :as text-button]
            [cdq.ui.scroll-pane-table-window :as scroll-pane-table-window]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn- k->label-str [k]
  (str "[LIGHT_GRAY]:"
       (when-let [ns (namespace k)] (str ns "/"))
       "[][WHITE]"
       (name k)
       "[]"))

(defn- v->text [v]
  (cond
   (or (keyword? v)
       (number? v)
       (boolean? v)
       (string? v))
   (str "[GOLD]" v "[]")

   :else
   (str (class v))))

(declare table-view-window)

; TODO isn't there a clojure data browser thingy library

(defn- v->actor [v]
  (if (map? v)
    (text-button/create "Map"
                        (fn [_actor {:keys [ctx/stage]}]
                          (stage/add! stage (table-view-window {:title "title"
                                                                :data v
                                                                :width 500
                                                                :height 500}))))
    {:actor/type :actor.type/label
     :label/text (v->text v)}))

(defn table-view-window [{:keys [title data width height]}]
  {:pre [(map? data)]}
  (scroll-pane-table-window/create {:title title
                                    :rows (for [[k v] (sort-by key data)]
                                            {:label (k->label-str k)
                                             :actor (v->actor v)})
                                    :width width
                                    :height height}))
