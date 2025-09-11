(ns cdq.editor.widgets)

(def k->methods*
  '{:s/animation {:create cdq.schema.animation/create
                  :value cdq.editor.widget.default/value}

    :s/boolean {:create cdq.schema.boolean/create
                :value cdq.schema.boolean/value}

    :s/enum {:create cdq.schema.enum/create
             :value cdq.schema.enum/value}

    :s/image {:create cdq.schema.image/create
              :value cdq.editor.widget.default/value}

    :s/map {:create cdq.schema.map/create
            :value cdq.schema.map/value}

    :s/number {:create cdq.editor.widget.edn/create
               :value cdq.editor.widget.edn/value}

    :s/one-to-many {:create cdq.schema.one-to-many/create
                    :value cdq.schema.one-to-many/value}

    :s/one-to-one {:create cdq.schema.one-to-one/create
                   :value cdq.schema.one-to-one/value}

    :s/qualified-keyword {:create cdq.editor.widget.default/create
                          :value cdq.editor.widget.default/value}

    :s/some {:create cdq.editor.widget.default/create
             :value cdq.editor.widget.default/value}

    :s/sound {:create cdq.schema.sound/create
              :value cdq.editor.widget.default/value}

    :s/string {:create cdq.schema.string/create
               :value cdq.schema.string/value}

    :s/val-max {:create cdq.editor.widget.edn/create
                :value cdq.editor.widget.edn/value}

    :s/vector {:create cdq.editor.widget.default/create
               :value cdq.editor.widget.default/value}
    })

(require 'cdq.effects)

(def k->methods
  (cdq.effects/walk-method-map k->methods*))
