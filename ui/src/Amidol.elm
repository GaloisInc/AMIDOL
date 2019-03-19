port module Amidol exposing (init, main)

import Browser
import Debug
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events exposing (..)
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html, div, img)
import Html.Attributes as HtmlAttr exposing (class, id, src)
import Json.Decode as Decode exposing (decodeValue, field)
import Json.Encode as Encode


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


port graphData : (Decode.Value -> msg) -> Sub msg


port selectNode : (String -> msg) -> Sub msg


port selectEdge : (String -> msg) -> Sub msg


port selectNone : (() -> msg) -> Sub msg



-- MODEL


type alias Model =
    { title : String
    , graph : Graph
    , selected : Selected
    , vars : Dict String String
    , newVar : String
    }


init : flags -> ( Model, Cmd Msg )
init flags =
    ( { title = "SIR"
      , graph = emptyGraph
      , selected = NoneSelected
      , vars =
            Dict.fromList
                [ ( "Model.Total pop.", "0" )
                , ( "S.Pop.", "0" )
                , ( "I.Pop.", "0" )
                , ( "R.Pop.", "0" )
                , ( "infect.beta", "" )
                , ( "cure.gamma", "" )
                ]
      , newVar = ""
      }
    , Cmd.none
    )


type Selected
    = SelectedNode String
    | SelectedEdge String
    | NoneSelected


type alias Graph =
    { nodes : Dict String Node
    , edges : Dict String Edge
    }


emptyGraph : Graph
emptyGraph =
    { nodes = Dict.empty, edges = Dict.empty }


decodeGraph : Decode.Value -> Graph
decodeGraph data =
    let
        decoder =
            Decode.map2 Graph
                (field "nodes" <| Decode.dict decodeNode)
                (field "edges" <| Decode.dict decodeEdge)
    in
    case decodeValue decoder data of
        Ok graph ->
            graph

        Err err ->
            emptyGraph



-- Debug.log (Decode.errorToString err) emptyGraph


type alias Node =
    { id : String
    , label : String
    , image : String
    , x : Float
    , y : Float
    }


decodeNode : Decode.Decoder Node
decodeNode =
    Decode.map5 Node
        (field "id" Decode.string)
        (field "label" Decode.string)
        (field "image" Decode.string)
        (field "x" Decode.float)
        (field "y" Decode.float)


type alias Edge =
    { id : String
    , label : String
    , from : String
    , to : String
    }


decodeEdge : Decode.Decoder Edge
decodeEdge =
    Decode.map4 Edge
        (field "id" Decode.string)
        (field "label" Decode.string)
        (field "from" Decode.string)
        (field "to" Decode.string)


encode : Model -> Encode.Value
encode model =
    let
        encodeNode node =
            Encode.object
                [ ( "id", Encode.string node.id )
                , ( "label", Encode.string node.label )
                , ( "image", Encode.string node.image )
                , ( "x", Encode.float node.x )
                , ( "y", Encode.float node.y )
                ]

        encodeEdge edge =
            Encode.object
                [ ( "id", Encode.string edge.id )
                , ( "label", Encode.string edge.label )
                , ( "from", Encode.string edge.from )
                , ( "to", Encode.string edge.to )
                ]
    in
    Encode.object
        [ ( "title", Encode.string model.title )
        , ( "nodes", Encode.dict identity encodeNode model.graph.nodes )
        , ( "edges", Encode.dict identity encodeEdge model.graph.edges )
        , ( "vars", Encode.dict identity Encode.string model.vars )
        ]



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ graphData GraphData
        , selectNode SelectNode
        , selectEdge SelectEdge
        , selectNone SelectNone
        ]



-- UPDATE


type Msg
    = GraphData Decode.Value
    | SelectNode String
    | SelectEdge String
    | SelectNone ()
    | ChangeTitle String
    | AddVar String
    | DeleteVar String
    | ChangeVar String String
    | ChangeNewVar String


sync : Dict String String -> Graph -> Graph -> Dict String String
sync vars oldGraph newGraph =
    let
        newNodesOnly =
            Dict.values <| Dict.diff newGraph.nodes oldGraph.nodes

        newNodeVars =
            Dict.fromList <|
                List.concat <|
                    List.map
                        (\node ->
                            [ ( node.id ++ ".Pop.", "0" )
                            ]
                        )
                        newNodesOnly

        newEdgesOnly =
            Dict.values <| Dict.diff newGraph.edges oldGraph.edges

        newEdgeVars =
            Dict.fromList <| List.concat <| List.map (\edge -> []) newEdgesOnly

        graphIds =
            Dict.keys newGraph.nodes ++ Dict.keys newGraph.edges

        ok var _ =
            String.startsWith "Model." var
                || List.any (\id -> String.startsWith (id ++ ".") var) graphIds
    in
    Dict.union (Dict.union newNodeVars newEdgeVars) (Dict.filter ok vars)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GraphData data ->
            let
                newGraph =
                    decodeGraph data
            in
            ( { model
                | graph = newGraph
                , vars = sync model.vars model.graph newGraph
                , selected = NoneSelected
              }
            , Cmd.none
            )

        SelectNode id ->
            ( { model | selected = SelectedNode id }, Cmd.none )

        SelectEdge id ->
            ( { model | selected = SelectedEdge id }, Cmd.none )

        SelectNone () ->
            ( { model | selected = NoneSelected }, Cmd.none )

        ChangeTitle newTitle ->
            ( { model | title = newTitle }, Cmd.none )

        AddVar key ->
            ( { model | vars = Dict.insert key "" model.vars, newVar = "" }, Cmd.none )

        DeleteVar key ->
            ( { model | vars = Dict.remove key model.vars }, Cmd.none )

        ChangeVar key value ->
            ( { model | vars = Dict.insert key value model.vars }, Cmd.none )

        ChangeNewVar newVarName ->
            ( { model | newVar = newVarName }, Cmd.none )



-- VIEW


header : String -> Element Msg
header title =
    row
        [ width fill
        , paddingXY 20 5
        , Border.widthEach { bottom = 2, top = 0, left = 0, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
        [ Input.text [ centerX, width <| px 250 ]
            { label =
                Input.labelLeft
                    [ Font.color <| rgb255 160 160 160
                    , centerY
                    ]
                <|
                    text "Model:"
            , onChange = ChangeTitle
            , placeholder = Nothing
            , text = title
            }
        ]


sidebar : Model -> Element Msg
sidebar { graph, vars, newVar, selected } =
    let
        varEl ( key, value ) =
            let
                unPrefixedKey =
                    String.concat <|
                        List.intersperse "." <|
                            Maybe.withDefault [] <|
                                List.tail <|
                                    String.split "." key
            in
            row
                [ spacing 10, padding 10, width fill ]
                [ Input.text
                    [ alignRight, width <| px 100 ]
                    { label = Input.labelLeft [ centerY ] <| text <| unPrefixedKey ++ " ="
                    , onChange = ChangeVar key
                    , placeholder = Nothing
                    , text = value
                    }
                , Input.button
                    []
                    -- paddingEach { top = 0, right = 10, bottom = 0, left = 0 } ]
                    { onPress = Just (DeleteVar key)
                    , label = el [ Font.color <| rgb255 160 160 160 ] <| text "⨯"
                    }
                ]

        title =
            el
                [ centerX
                , paddingXY 0 20
                , Font.color <| rgb255 160 160 160
                ]
            <|
                text <|
                    (case selected of
                        SelectedNode id ->
                            Maybe.withDefault "" <|
                                Maybe.map .label <|
                                    Dict.get id graph.nodes

                        SelectedEdge id ->
                            Maybe.withDefault "" <|
                                Maybe.map .label <|
                                    Dict.get id graph.edges

                        NoneSelected ->
                            "Model"
                    )
                        ++ " variables:"

        scope =
            case selected of
                SelectedNode id ->
                    id ++ "."

                SelectedEdge id ->
                    id ++ "."

                NoneSelected ->
                    "Model."

        prefixed key _ =
            String.startsWith scope key

        adder =
            row
                [ spacing 20, padding 20, width fill ]
                [ Input.text
                    [ alignRight, width fill ]
                    { label = Input.labelRight [] <| none
                    , onChange = ChangeNewVar
                    , placeholder = Nothing
                    , text = newVar
                    }
                , Input.button
                    [ width <| px 100 ]
                    { onPress =
                        if newVar == "" then
                            Nothing

                        else
                            Just <| AddVar <| scope ++ newVar
                    , label =
                        el
                            [ Font.color <| rgb255 160 160 160
                            , Font.size 30
                            ]
                        <|
                            text "+"
                    }
                ]
    in
    column
        [ height fill
        , width <| fillPortion 1

        -- , spacingXY 0 10
        , Border.widthEach { bottom = 0, top = 0, left = 2, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
    <|
        [ title ]
            ++ (List.map varEl <| Dict.toList <| Dict.filter prefixed vars)
            ++ [ adder ]


exposedDiv : String -> List (Attribute msg) -> List (Html msg) -> Element msg
exposedDiv id attrs children =
    el attrs <| html <| div [ HtmlAttr.id id ] children


graphPanel : Element Msg
graphPanel =
    let
        palette =
            exposedDiv "palette"
                [ width fill ]
                [ img
                    [ HtmlAttr.id "undo_button"
                    , HtmlAttr.class "undo-redo"
                    , HtmlAttr.src "images/undo.png"
                    ]
                    []
                , img
                    [ HtmlAttr.id "redo_button"
                    , HtmlAttr.class "undo-redo"
                    , HtmlAttr.src "images/redo.png"
                    ]
                    []
                , div
                    [ HtmlAttr.class "spacer" ]
                    []
                , img
                    [ HtmlAttr.src "images/person.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/sick.jpg"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/happy.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                ]
    in
    el [ height fill, width <| fillPortion 4 ] <|
        column [ height fill, width fill, padding 20 ]
            [ palette
            , exposedDiv "graph" [ width fill, height <| px 600 ] []
            ]


view : Model -> Html Msg
view model =
    layout [ height fill ] <|
        column [ height fill, width fill ]
            [ header model.title
            , row [ height fill, width fill ]
                [ graphPanel, sidebar model ]
            ]
