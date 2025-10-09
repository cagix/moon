(ns cdq.ui.editor.window
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.ui.editor.schema :as schema]
            [clojure.gdx :as gdx]
            [clojure.throwable :as throwable]
            [cdq.ui :as ui]
            [cdq.ui.widget :as widget]
            [clojure.scene2d.vis-ui.window :as vis-window]
            [clojure.scene2d.vis-ui.text-button :as text-button]
            [cdq.ui.window :as window])
  (:import (com.badlogic.gdx Input$Keys)
           (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- with-window-close [f]
  (fn [actor {:keys [ctx/stage]
              :as ctx}]
    (try
     (let [new-ctx (update ctx :ctx/db f)
           stage (Actor/.getStage actor)]
       (set! (.ctx stage) new-ctx))
     (Actor/.remove (window/find-ancestor actor))
     (catch Throwable t
       (throwable/pretty-pst t)
       (ui/show-error-window! stage t)))))

(defn- update-property-fn [get-widget-value]
  (fn [db]
    (db/update! db (get-widget-value))))

(defn- delete-property-fn [property-id]
  (fn [db]
    (db/delete! db property-id)))

(defn- create*
  [{:keys [scroll-pane-height
           widget
           get-widget-value
           property-id]}]
  (let [clicked-delete-fn (with-window-close (delete-property-fn property-id))
        clicked-save-fn   (with-window-close (update-property-fn get-widget-value))
        actors [(proxy [Actor] []
                  (act [_delta]
                    (when-let [stage (.getStage this)]
                      (let [{:keys [ctx/gdx]
                             :as ctx} (.ctx stage)]
                        (when (gdx/key-just-pressed? gdx Input$Keys/ENTER)
                          (clicked-save-fn this ctx))))))]
        save-button (text-button/create
                     {:text "Save [LIGHT_GRAY](ENTER)[]"
                      :on-clicked clicked-save-fn})
        delete-button (text-button/create
                       {:text "Delete"
                        :on-clicked clicked-delete-fn})
        scroll-pane-rows [[{:actor widget :colspan 2}]
                          [{:actor save-button :center? true}
                           {:actor delete-button :center? true}]]
        rows [[(widget/scroll-pane-cell scroll-pane-height
                                        scroll-pane-rows)]]]
    {:title "[SKY]Property[]"
     :actor/name "cdq.ui.editor.window"
     :modal? true
     :close-button? true
     :center? true
     :close-on-escape? true
     :group/actors actors
     :rows rows
     :cell-defaults {:pad 5}
     :pack? true}))

(defn create
  [{:keys [ctx
           property]}]
  (let [{:keys [ctx/db
                ctx/stage]} ctx
        schemas (:db/schemas db)
        schema (get schemas (property/type property))
        ; build for get-widget-value
        ; or find a way to find the widget from the context @ save button
        ; should be possible
        widget (schema/create schema property ctx)]
    (vis-window/create
     (create* {:scroll-pane-height (ui/viewport-height stage)
               :widget widget
               :get-widget-value #(schema/value schema widget schemas)
               :property-id (:property/id property)}))))
