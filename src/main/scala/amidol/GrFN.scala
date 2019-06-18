package amidol


object GrFN {

  type string = String
  case class source_code_file_path(path: String)

  case class grfn_spec(
    date_created: string,
    source: List[source_code_file_path],
    start: List[string],
    identifiers: List[identifier_spec]
  )

  case class base_name(name: String)
  case class scope_path(path: String)
  case class namespace_path(path: String)
  case class source_code_reference(ref: String)
  case class gensym(id: String) // letter followed by a unique integer.

  case class identifier_spec(
    base_name: base_name,
    scope: scope_path,
    namespace: namespace_path,
    source_references: List[source_code_reference],
    gensym: gensym,
    grounding: List[grounding_metadata_spec]
  )

  sealed trait grounding_metadata_spec_type
  case object definition extends grounding_metadata_spec_type
  case object units extends grounding_metadata_spec_type
  case object constraint extends grounding_metadata_spec_type

  case class grounding_metadata_spec(
    source: string,
    `type`: grounding_metadata_spec_type,
    value: string,
    cariables: List[variable_spec]
  )

  case class variable_name(identifier: String) extends index_range_value with function_reference_spec_io

  sealed trait variable_domain_type
  case object real extends variable_domain_type
  case object integer extends variable_domain_type with index_range_value
  case object string extends variable_domain_type

  sealed trait boolean extends variable_domain_type
  case object True extends boolean
  case object False extends boolean

  case class variable_spec(
    name: variable_name,
    domain: variable_domain_type,
    mutable: boolean,
    functions: List[function_spec]
  )

  sealed trait function_spec


  sealed trait function_source_reference_name
  case class function_name(identifier: String) extends function_source_reference_name


  sealed trait function_assign_spec_type
  case object assign extends function_assign_spec_type
  case object condition extends function_assign_spec_type
  case object decision extends function_assign_spec_type

  sealed trait function_source_reference_type
  case object function extends function_source_reference_type
  case object variable extends function_source_reference_type

  sealed trait function_assign_spec_sources_or_targets
  case class function_source_reference(
    name: function_source_reference,
    `type`: function_source_reference_type
  ) extends function_assign_spec_sources_or_targets
  case object variable_name extends function_assign_spec_sources_or_targets with function_source_reference_name

  sealed trait type_inhabitants
  case object literal extends type_inhabitants

  case class literal_value(
    dtype: variable_domain_type,
    value: string
  )

  sealed trait function_assign_spec_body
  case class function_assign_body_literal_spec(
    `type`: literal.type,
    value: literal_value
  ) extends function_assign_spec_body

  case object lambda extends type_inhabitants // expecting Julia code here.
  type lambda_function_reference = string  // denoting the python function in lambdas.py

  case class function_assign_body_lambda_spec(
    `type`: lambda.type,
    name: function_name,
    reference: lambda_function_reference
  ) extends function_assign_spec_body


  case class function_reference_spec(
    function: function_name,
    input: List[function_reference_spec_io],
    output: List[function_reference_spec_io]
  )

  sealed trait function_reference_spec_io


  case class function_assign_spec(
    name: function_name,
    `type`: function_assign_spec_type,
    sources: List[function_assign_spec_sources_or_targets],
    target: function_assign_spec_sources_or_targets,
    body: function_reference_spec
  ) extends function_spec

  case class function_container_spec(
    name: function_name,
    `type`: function_assign_spec_type,
    sources: List[function_assign_spec_sources_or_targets],
    target: function_assign_spec_sources_or_targets,
    body: List[function_reference_spec]
  ) extends function_spec

  case object loop_plate extends type_inhabitants

  sealed trait index_range_value
  type integer = Integer
  case class variable_reference(
    variable: variable_name,
    index: integer
  ) extends index_range_value with function_reference_spec_io   // typo!
  // ...and: varaible_name
  case class index_range(start: Either[integer, index_range_value], end: Either[integer, index_range_value])

  case class loop_condition() // I DON'T KNOW WHAT THIS IS!!!!!!

  case class function_loop_plate_spec(
    name: function_name,
    `type`: loop_plate.type,
    input: List[variable_name],
    index_variable: variable_name,
    index_iteration_range: index_range,
    condition: loop_condition,
    body: List[function_reference_spec]
  ) extends function_spec


  grfn_spec("", Nil, Nil, Nil)
}
