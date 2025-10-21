(ns cdq.ui.build.editor-window
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.ui.editor.schema :as schema]
            [cdq.ui.stage :as stage]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.widget :as widget]
            [cdq.ui.window :as window]
            [clojure.gdx.input.keys :as input.keys]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.throwable :as throwable]))

(defmethod stage/build :actor/editor-window
  [{:keys [ctx
           property]}]
  (let [{:keys [ctx/db
                ctx/stage]} ctx
        schemas (:db/schemas db)
        schema (get schemas (property/type property))
        ; build for get-widget-value
        ; or find a way to find the widget from the context @ save button
        ; should be possible
        widget (schema/create schema property ctx)
        scroll-pane-height (ui/viewport-height stage)
        get-widget-value #(schema/value schema widget schemas)
        property-id (:property/id property)
        with-window-close (fn [f]
                            (fn [actor {:keys [ctx/stage]
                                        :as ctx}]
                              (try
                               (let [new-ctx (update ctx :ctx/db f)
                                     stage (actor/stage actor)]
                                 (stage/set-ctx! stage new-ctx))
                               (actor/remove! (window/find-ancestor actor))
                               (catch Throwable t
                                 (throwable/pretty-pst t)
                                 (ui/show-error-window! stage t)))))
        clicked-delete-fn (with-window-close (fn [db]
                                               (db/delete! db property-id)))
        clicked-save-fn (with-window-close (fn [db]
                                             (db/update! db (get-widget-value))))
        actors [(actor/create
                 {:act (fn [this delta]
                         (when-let [stage (actor/stage this)]
                           (let [{:keys [ctx/input]
                                  :as ctx} (stage/ctx stage)]
                             (when (input/key-just-pressed? input input.keys/enter)
                               (clicked-save-fn this ctx)))))
                  :draw (fn [this batch parent-alpha])})]
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
    (window/create
     {:title "[SKY]Property[]"
      :actor/name "cdq.ui.editor.window"
      :modal? true
      :close-button? true
      :center? true
      :close-on-escape? true
      :group/actors actors
      :rows rows
      :cell-defaults {:pad 5}
      :pack? true})))
