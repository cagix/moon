(ns cdq.application.create.db.schemas)

(def schema-fn-map
  '{
    :s/animation {cdq.db.schema/malli-form   cdq.db.schema.animation/malli-form
                  cdq.db.schema/create-value cdq.db.schema.animation/create-value
                  cdq.db.schema/create       cdq.db.schema.animation/create
                  cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/boolean {cdq.db.schema/malli-form   cdq.db.schema.boolean/malli-form
                cdq.db.schema/create-value cdq.db.schema.boolean/create-value
                cdq.db.schema/create       cdq.db.schema.boolean/create
                cdq.db.schema/value        cdq.db.schema.boolean/value}

    :s/enum {cdq.db.schema/malli-form   cdq.db.schema.enum/malli-form
             cdq.db.schema/create-value cdq.db.schema.enum/create-value
             cdq.db.schema/create       cdq.db.schema.enum/create
             cdq.db.schema/value        cdq.db.schema.enum/value}

    :s/image {cdq.db.schema/malli-form   cdq.db.schema.image/malli-form
              cdq.db.schema/create-value cdq.db.schema.image/create-value
              cdq.db.schema/create       cdq.db.schema.image/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/map {cdq.db.schema/malli-form   cdq.db.schema.map/malli-form
            cdq.db.schema/create-value cdq.db.schema.map/create-value
            cdq.db.schema/create       cdq.db.schema.map/create
            cdq.db.schema/value        cdq.db.schema.map/value}

    :s/number {cdq.db.schema/malli-form   cdq.db.schema.number/malli-form
               cdq.db.schema/create-value cdq.db.schema.number/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.edn/create
               cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.db.schema/malli-form   cdq.db.schema.one-to-many/malli-form
                    cdq.db.schema/create-value cdq.db.schema.one-to-many/create-value
                    cdq.db.schema/create       cdq.db.schema.one-to-many/create
                    cdq.db.schema/value        cdq.db.schema.one-to-many/value}

    :s/one-to-one {cdq.db.schema/malli-form   cdq.db.schema.one-to-one/malli-form
                   cdq.db.schema/create-value cdq.db.schema.one-to-one/create-value
                   cdq.db.schema/create       cdq.db.schema.one-to-one/create
                   cdq.db.schema/value        cdq.db.schema.one-to-one/value}

    :s/qualified-keyword {cdq.db.schema/malli-form   cdq.db.schema.qualified-keyword/malli-form
                          cdq.db.schema/create-value cdq.db.schema.qualified-keyword/create-value
                          cdq.db.schema/create       cdq.ui.editor.widget.default/create
                          cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/some {cdq.db.schema/malli-form   cdq.db.schema.some/malli-form
             cdq.db.schema/create-value cdq.db.schema.some/create-value
             cdq.db.schema/create       cdq.ui.editor.widget.default/create
             cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/sound {cdq.db.schema/malli-form   cdq.db.schema.sound/malli-form
              cdq.db.schema/create-value cdq.db.schema.sound/create-value
              cdq.db.schema/create       cdq.db.schema.sound/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/string {cdq.db.schema/malli-form   cdq.db.schema.string/malli-form
               cdq.db.schema/create-value cdq.db.schema.string/create-value
               cdq.db.schema/create       cdq.db.schema.string/create
               cdq.db.schema/value        cdq.db.schema.string/value}

    :s/val-max {cdq.db.schema/malli-form   cdq.db.schema.val-max/malli-form
                cdq.db.schema/create-value cdq.db.schema.val-max/create-value
                cdq.db.schema/create       cdq.ui.editor.widget.edn/create
                cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/vector {cdq.db.schema/malli-form   cdq.db.schema.vector/malli-form
               cdq.db.schema/create-value cdq.db.schema.vector/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.default/create
               cdq.db.schema/value        cdq.ui.editor.widget.default/value}
    }
  )

(alter-var-root #'schema-fn-map update-vals (fn [method-map]
                                              (update-vals method-map
                                                           (fn [sym]
                                                             (let [avar (requiring-resolve sym)]
                                                               (assert avar sym)
                                                               avar)))))
