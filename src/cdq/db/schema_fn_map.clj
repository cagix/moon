(ns cdq.db.schema-fn-map)

(def fn-map
  '{
    :s/animation {cdq.ui.editor.schema/create       cdq.ui.editor.schema.animation/create
                  cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}

    :s/boolean {cdq.ui.editor.schema/create       cdq.ui.editor.schema.boolean/create
                cdq.ui.editor.schema/value        cdq.ui.editor.schema.boolean/value}

    :s/enum {cdq.ui.editor.schema/create       cdq.ui.editor.schema.enum/create
             cdq.ui.editor.schema/value        cdq.ui.editor.schema.enum/value}

    :s/image {cdq.ui.editor.schema/create       cdq.ui.editor.schema.image/create
              cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}

    :s/map {cdq.ui.editor.schema/create       cdq.ui.editor.schema.map/create
            cdq.ui.editor.schema/value        cdq.ui.editor.schema.map/value}

    :s/number {cdq.ui.editor.schema/create       cdq.ui.editor.widget.edn/create
               cdq.ui.editor.schema/value        cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.ui.editor.schema/create       cdq.ui.editor.schema.one-to-many/create
                    cdq.ui.editor.schema/value        cdq.ui.editor.schema.one-to-many/value}

    :s/one-to-one {cdq.ui.editor.schema/create       cdq.ui.editor.schema.one-to-one/create
                   cdq.ui.editor.schema/value        cdq.ui.editor.schema.one-to-one/value}

    :s/qualified-keyword {cdq.ui.editor.schema/create       cdq.ui.editor.widget.default/create
                          cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}

    :s/some {cdq.ui.editor.schema/create       cdq.ui.editor.widget.default/create
             cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}

    :s/sound {cdq.ui.editor.schema/create       cdq.ui.editor.schema.sound/create
              cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}

    :s/string {cdq.ui.editor.schema/create       cdq.ui.editor.schema.string/create
               cdq.ui.editor.schema/value        cdq.ui.editor.schema.string/value}

    :s/val-max {cdq.ui.editor.schema/create       cdq.ui.editor.widget.edn/create
                cdq.ui.editor.schema/value        cdq.ui.editor.widget.edn/value}

    :s/vector {cdq.ui.editor.schema/create       cdq.ui.editor.widget.default/create
               cdq.ui.editor.schema/value        cdq.ui.editor.widget.default/value}
    }
  )

(alter-var-root #'fn-map update-vals (fn [method-map]
                                       (update-vals method-map
                                                    (fn [sym]
                                                      (let [avar (requiring-resolve sym)]
                                                        (assert avar sym)
                                                        avar)))))
