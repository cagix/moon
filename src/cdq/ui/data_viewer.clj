(ns cdq.ui.data-viewer
  (:require [cdq.ui.scroll-pane-table-window :as scroll-pane-table-window]
            [cdq.ui.stage :as stage]
            [cdq.ui.text-button :as text-button]))

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

(declare create)

(defn- v->actor [v]
  (if (map? v)
    (text-button/create "Map"
                        (fn [_actor {:keys [ctx/stage]}]
                          (stage/add! stage (create {:title "title"
                                                     :data v
                                                     :width 500
                                                     :height 500}))))
    {:actor/type :actor.type/label
     :label/text (v->text v)}))

(defn create
  [{:keys [title
           data
           width
           height]}]
  {:pre [(map? data)]}
  (scroll-pane-table-window/create {:title title
                                    :rows (for [[k v] (sort-by key data)]
                                            {:label (k->label-str k)
                                             :actor (v->actor v)})
                                    :width width
                                    :height height}))
