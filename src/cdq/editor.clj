(ns cdq.editor
  (:require [cdq.application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.editor.property :as property]
            [cdq.editor.overview-table :as overview-table]
            [cdq.ui.widget]
            [cdq.utils :refer [pprint-to-str]]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.window :as window]))

(defn- with-window-close [f]
  (fn [actor ctx]
    (try (f)
         (actor/remove! (window/find-ancestor actor))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))))

(defn- update-property-fn [state get-widget-value]
  (fn []
    (swap! state update :ctx/db db/update! (get-widget-value))))

(defn- delete-property-fn [state property-id]
  (fn []
    (swap! state update :ctx/db db/delete! property-id)))

(defn create-button-handlers [state property-id get-widget-value]
  {:clicked-delete-fn (with-window-close (delete-property-fn state property-id))
   :clicked-save-fn   (with-window-close (update-property-fn state get-widget-value))})

(defn overview-table
  [{:keys [ctx/db
           ctx/graphics]}
   property-type
   clicked-id-fn]
  (let [{:keys [sort-by-fn
                extra-info-text
                columns
                image-scale]} (cdq.application/property-overview property-type)]
    (->> (db/all-raw db property-type)
         (sort-by sort-by-fn)
         (map (fn [property]
                {:texture-region (graphics/texture-region graphics (property/image property))
                 :on-clicked (fn [_actor ctx]
                               (clicked-id-fn (:property/id property) ctx))
                 :tooltip (pprint-to-str property)
                 :extra-info-text (extra-info-text property)}))
         (partition-all columns)
         (overview-table/create image-scale))))
