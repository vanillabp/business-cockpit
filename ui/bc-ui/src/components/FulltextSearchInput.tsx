import {debounce, TranslationFunction, useResponsiveScreen} from "@vanillabp/bc-shared";
import {ReactNode, useMemo, useRef, useState} from "react";
import {Box, Grid, Text, TextInput, Tip} from "grommet";
import {Clear, Search} from "grommet-icons";

interface Suggestion {
    label: ReactNode;
    value: string | undefined;
}

const FulltextSearchInput = ({
                            t,
                            initialQuery,
                            limitListToKwic,
                            kwic,
                        }: {
    t: TranslationFunction,
    initialQuery: (columnPath?: string) => string,
    limitListToKwic: (columnPath: string | undefined, query?: string) => void,
    kwic: (columnPath: string | undefined, query: string) => Promise<Array<{ item: string, count: number }>>,
    // focus?: boolean,
}) => {
    const { isPhone } = useResponsiveScreen();
    const textFieldRef = useRef<HTMLInputElement>(null);
    const [ query, setQuery ] = useState(initialQuery)
    const currentQuery = useRef<string>(initialQuery(undefined));
    const [ suggestions, setSuggestions ] =
        useState<Array<Suggestion> | undefined>(undefined);
    const ignoreKeyEnter = useRef(false);
    const select = (value: string, suggestion: boolean) => {
        if (suggestion) {
            ignoreKeyEnter.current  = true;
        } else if (ignoreKeyEnter.current) {
            ignoreKeyEnter.current = false;
            return;
        }
        if (value === undefined) return;
        setSuggestions(undefined);
        setQuery(value);
        currentQuery.current = value;
        limitListToKwic(undefined, value);
    };
    const kwicDebounced = useMemo(() => debounce(async () => {
        const result = await kwic(undefined, currentQuery.current);
        const newSuggestions = result
            .map(r => ({
                value: r.item,
                label: <Box
                    direction="row"
                    justify="between"
                    pad="xsmall">
                    <Text
                        weight="bold"
                        truncate="tip">
                        { r.item }
                    </Text>
                    <Box
                        align="right">
                        { r.count }
                    </Box>
                </Box> }));
        setSuggestions(
            newSuggestions.length > 20
                ? [ ...newSuggestions.slice(0, 20), { value: undefined, label: <Box pad="xsmall">{ t('kwic_to-many-hits') }</Box> } ]
                : newSuggestions);
    }, 300), [ currentQuery, setSuggestions ]);
    const updateResult = (newQuery: string) => {
        currentQuery.current = newQuery;
        setQuery(newQuery);
        if (newQuery.length < 3) {
            if (suggestions) {
                setSuggestions(undefined);
            }
            return;
        }
        kwicDebounced();
    };
    const clear = () => {
        currentQuery.current = '';
        setQuery('');
        setSuggestions(undefined);
        textFieldRef.current!.focus();
        limitListToKwic(undefined, undefined);
    };

    return (
        <Box
            width={ isPhone ? "100%" : "min(100%, 18rem)" }
            elevation="small"
            alignContent="center"
            hoverIndicator={ "white" }
            focusIndicator={ false }
            round={ { size: '0.4rem' } }
            border={ { color: "dark-4" } }>
            <Grid
                columns={ [ 'auto', '2rem' ] }
                fill>
                <Box
                    pad={ { horizontal: '0.4rem', vertical: '0.25rem' } }
                    direction="row"
                    justify="center">
                    <TextInput
                        plain="full"
                        ref={ textFieldRef }
                        value={ query }
                        placeholder={ t('kwic_placeholder') }
                        onKeyDown={ event => event.key === 'Enter' ? select(query, false) : undefined }
                        onChange={ event => updateResult(event.target.value) }
                        suggestions={ suggestions === undefined
                            ? []
                            : suggestions.length === 0
                                ? [ { value: undefined, label: <Box pad="xsmall">{ t('kwic_no-hit') }</Box> } ]
                                : suggestions }
                        onSuggestionSelect={ x => select(x.suggestion.value, true) }
                        focusIndicator={ false }
                        reverse />
                </Box>
                <Tip
                    content={ t('kwic_tooltip') }>
                    <Box
                        align="center"
                        justify="center">
                        {
                            Boolean(query)
                                ? <Clear
                                    onMouseUp={ clear }
                                    color="dark-4" />
                                : <Search
                                    color="dark-4" />
                        }
                    </Box>
                </Tip>
            </Grid>
        </Box>);
}

export { FulltextSearchInput };